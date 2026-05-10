package pro.sketchware.activities.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import pro.sketchware.R;

public class ChatPagerAdapter extends FragmentStatePagerAdapter {
    private final ChatActivity activity;
    private final ChatMessagesFragment messagesFragment;
    private final ChatDiffFragment diffFragment;

    public ChatPagerAdapter(@NonNull ChatActivity activity,
                            @NonNull ChatMessagesFragment messagesFragment,
                            @NonNull ChatDiffFragment diffFragment) {
        super(activity.getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.activity = activity;
        this.messagesFragment = messagesFragment;
        this.diffFragment = diffFragment;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return switch (position) {
            case 1 -> diffFragment;
            default -> messagesFragment;
        };
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return switch (position) {
            case 1 -> activity.getString(R.string.chat_page_diffs);
            default -> activity.getString(R.string.chat_page_chat);
        };
    }
}
