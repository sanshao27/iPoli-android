package io.ipoli.android.quest.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import javax.inject.Inject;

import io.ipoli.android.Constants;
import io.ipoli.android.app.App;
import io.ipoli.android.app.utils.IntentUtils;
import io.ipoli.android.quest.Quest;
import io.ipoli.android.quest.persistence.QuestPersistenceService;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 2/1/16.
 */
public class ScheduleQuestReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_SCHEDULE_REMINDER = "io.ipoli.android.intent.action.SCHEDULE_QUEST_REMINDER";

    @Inject
    QuestPersistenceService questPersistenceService;

    @Override
    public void onReceive(Context context, Intent intent) {
        App.getAppComponent(context).inject(this);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        cancelScheduledReminder(context, alarm);

        Quest q = questPersistenceService.findPlannedQuestStartingAfter(new Date());
        if (q == null) {
            return;
        }
        scheduleNextReminder(context, alarm, q);
    }

    private void cancelScheduledReminder(Context context, AlarmManager alarm) {
        alarm.cancel(getCancelPendingIntent(context));
    }

    private void scheduleNextReminder(Context context, AlarmManager alarm, Quest q) {
        Intent i = new Intent(context, RemindStartQuestReceiver.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, q.getId());
        PendingIntent pendingIntent = IntentUtils.getBroadcastPendingIntent(context, i);
        alarm.setExact(AlarmManager.RTC_WAKEUP, Quest.getStartDateTime(q).getTime(), pendingIntent);
    }

    public PendingIntent getCancelPendingIntent(Context context) {
        Intent i = new Intent(context, RemindStartQuestReceiver.class);
        return IntentUtils.getBroadcastPendingIntent(context, i);
    }
}
