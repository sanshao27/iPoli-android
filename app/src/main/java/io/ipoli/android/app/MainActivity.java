package io.ipoli.android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.mobiwise.materialintro.shape.Focus;
import io.ipoli.android.Constants;
import io.ipoli.android.R;
import io.ipoli.android.app.events.ContactUsClickEvent;
import io.ipoli.android.app.events.FeedbackClickEvent;
import io.ipoli.android.app.events.UndoCompletedQuestEvent;
import io.ipoli.android.app.ui.MainViewPager;
import io.ipoli.android.app.utils.EmailUtils;
import io.ipoli.android.quest.Difficulty;
import io.ipoli.android.quest.Quest;
import io.ipoli.android.quest.activities.AddQuestActivity;
import io.ipoli.android.quest.activities.InboxActivity;
import io.ipoli.android.quest.activities.QuestActivity;
import io.ipoli.android.quest.activities.QuestCompleteActivity;
import io.ipoli.android.quest.events.CompleteQuestRequestEvent;
import io.ipoli.android.quest.events.ShowQuestEvent;
import io.ipoli.android.quest.events.UndoCompletedQuestRequestEvent;
import io.ipoli.android.quest.fragments.CalendarDayFragment;
import io.ipoli.android.quest.fragments.OverviewFragment;
import io.ipoli.android.quest.persistence.QuestPersistenceService;
import io.ipoli.android.tutorial.Tutorial;
import io.ipoli.android.tutorial.TutorialItem;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 2/16/16.
 */
public class MainActivity extends BaseActivity {
    private static final int CALENDAR_TAB_POSITION = 0;
    private static final int OVERVIEW_TAB_POSITION = 1;

    @Inject
    Bus eventBus;

    @Inject
    QuestPersistenceService questPersistenceService;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.tabLayout)
    TabLayout tabLayout;

    @Bind(R.id.viewpager)
    MainViewPager viewPager;

    private View inboxButton;
    private View feedbackView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(new SimpleDateFormat(getString(R.string.today_date_format), Locale.getDefault()).format(new Date()));
        }

        appComponent().inject(this);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new CalendarDayFragment());
        viewPagerAdapter.addFragment(new OverviewFragment());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setPagingEnabled(false);
        tabLayout.setupWithViewPager(this.viewPager);
        tabLayout.getTabAt(CALENDAR_TAB_POSITION).setIcon(R.drawable.ic_today_white_24dp);
        tabLayout.getTabAt(OVERVIEW_TAB_POSITION).setIcon(R.drawable.ic_assignment_white_24dp);
        Tutorial.getInstance(this).addItem(
                new TutorialItem.Builder(this)
                        .setState(Tutorial.State.TUTORIAL_START_OVERVIEW)
                        .setTarget(((ViewGroup) tabLayout.getChildAt(0)).getChildAt(1))
                        .enableDotAnimation(false)
                        .setFocusType(Focus.MINIMUM)
                        .build());

        Tutorial.getInstance(this).addItem(
                new TutorialItem.Builder(this)
                        .setState(Tutorial.State.TUTORIAL_START_ADD_QUEST)
                        .setTarget(findViewById(R.id.add_quest))
                        .enableDotAnimation(true)
                        .performClick(true)
                        .setFocusType(Focus.MINIMUM)
                        .build());
    }

    @OnClick(R.id.add_quest)
    public void onAddQuest(View view) {
        Intent i = new Intent(this, AddQuestActivity.class);
        if (tabLayout.getSelectedTabPosition() == CALENDAR_TAB_POSITION) {
            i.putExtra(Constants.IS_TODAY_QUEST_EXTRA_KEY, true);
        }
        startActivity(i);
        overridePendingTransition(R.anim.slide_up_interpolate, 0);
    }

    @Subscribe
    public void onShowQuestEvent(ShowQuestEvent e) {
        Intent i = new Intent(this, QuestActivity.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, e.quest.getId());
        startActivity(i);
    }

    @Subscribe
    public void onQuestCompleteRequest(CompleteQuestRequestEvent e) {
        Intent i = new Intent(this, QuestCompleteActivity.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, e.quest.getId());
        startActivityForResult(i, Constants.COMPLETE_QUEST_RESULT_REQUEST_CODE);
    }

    @Subscribe
    public void onUndoCompletedQuestRequest(UndoCompletedQuestRequestEvent e) {
        Quest quest = e.quest;
        quest.setLog("");
        quest.setDifficulty(Difficulty.UNKNOWN.name());
        quest.setActualStartDateTime(null);
        quest.setMeasuredDuration(0);
        quest.setCompletedAtDateTime(null);
        quest = questPersistenceService.save(quest);
        eventBus.post(new UndoCompletedQuestEvent(quest));
        Toast.makeText(this, "Quest undone", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                inboxButton = findViewById(R.id.action_inbox);
                feedbackView = findViewById(R.id.action_feedback);
                Tutorial.getInstance(MainActivity.this).addItem(new TutorialItem.Builder(MainActivity.this)
                        .setTarget(inboxButton)
                        .setFocusType(Focus.MINIMUM)
                        .enableDotAnimation(false)
                        .setState(Tutorial.State.TUTORIAL_START_INBOX)
                        .build());
                Tutorial.getInstance(MainActivity.this).addItem(new TutorialItem.Builder(MainActivity.this)
                        .setTarget(feedbackView)
                        .setFocusType(Focus.MINIMUM)
                        .enableDotAnimation(false)
                        .performClick(false)
                        .dismissOnTouch(true)
                        .setState(Tutorial.State.TUTORIAL_VIEW_FEEDBACK)
                        .build());
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_inbox:
                startInboxActivity();
                break;
            case R.id.action_feedback:
                eventBus.post(new FeedbackClickEvent());
                EmailUtils.send(this, getString(R.string.feedback_email_subject), getString(R.string.feedback_email_chooser_title));
                break;
            case R.id.action_contact_us:
                eventBus.post(new ContactUsClickEvent());
                EmailUtils.send(this, getString(R.string.contact_us_email_subject), getString(R.string.contact_us_email_chooser_title));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        eventBus.register(this);
    }

    private void startInboxActivity() {
        startActivity(new Intent(this, InboxActivity.class));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
    }

    @Override
    public void onPause() {
        eventBus.unregister(this);
        super.onPause();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment) {
            fragments.add(fragment);
        }
    }
}