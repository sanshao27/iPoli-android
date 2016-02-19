package io.ipoli.android.quest.events;

import io.ipoli.android.quest.Quest;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 2/19/16.
 */
public class MoveQuestToCalendarRequestEvent {
    public final Quest quest;

    public MoveQuestToCalendarRequestEvent(Quest quest) {
        this.quest = quest;
    }
}
