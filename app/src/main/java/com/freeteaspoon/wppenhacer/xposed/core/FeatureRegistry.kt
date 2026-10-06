package com.freeteaspoon.wppenhacer.xposed.core

import com.freeteaspoon.wppenhacer.xposed.features.customization.BubbleColors
import com.freeteaspoon.wppenhacer.xposed.features.customization.ContactVerify
import com.freeteaspoon.wppenhacer.xposed.features.customization.CustomThemeV2
import com.freeteaspoon.wppenhacer.xposed.features.customization.CustomTime
import com.freeteaspoon.wppenhacer.xposed.features.customization.CustomToolbar
import com.freeteaspoon.wppenhacer.xposed.features.customization.CustomView
import com.freeteaspoon.wppenhacer.xposed.features.customization.DefaultEmoji
import com.freeteaspoon.wppenhacer.xposed.features.customization.FilterGroups
import com.freeteaspoon.wppenhacer.xposed.features.customization.FloatingBottomBar
import com.freeteaspoon.wppenhacer.xposed.features.customization.HideSeenView
import com.freeteaspoon.wppenhacer.xposed.features.customization.HideHomeViews
import com.freeteaspoon.wppenhacer.xposed.features.customization.HideHomeCamera
import com.freeteaspoon.wppenhacer.xposed.features.customization.HideTabs
import com.freeteaspoon.wppenhacer.xposed.features.customization.IGStatus
import com.freeteaspoon.wppenhacer.xposed.features.customization.SeparateGroup
import com.freeteaspoon.wppenhacer.xposed.features.customization.ShowOnline
import com.freeteaspoon.wppenhacer.xposed.features.general.AboutContactPicker
import com.freeteaspoon.wppenhacer.xposed.features.general.AntiRevoke
import com.freeteaspoon.wppenhacer.xposed.features.general.CallType
import com.freeteaspoon.wppenhacer.xposed.features.general.CaptureDevice
import com.freeteaspoon.wppenhacer.xposed.features.general.ChatLimit
import com.freeteaspoon.wppenhacer.xposed.features.general.DeleteStatus
import com.freeteaspoon.wppenhacer.xposed.features.general.NewChat
import com.freeteaspoon.wppenhacer.xposed.features.general.CallsButton
import com.freeteaspoon.wppenhacer.xposed.features.general.Others
import com.freeteaspoon.wppenhacer.xposed.features.general.PinnedLimit
import com.freeteaspoon.wppenhacer.xposed.features.general.SeenTick
import com.freeteaspoon.wppenhacer.xposed.features.general.ShareLimit
import com.freeteaspoon.wppenhacer.xposed.features.general.ShowEditMessage
import com.freeteaspoon.wppenhacer.xposed.features.general.Tasker
import com.freeteaspoon.wppenhacer.xposed.features.listeners.ContactItemListener
import com.freeteaspoon.wppenhacer.xposed.features.listeners.ConversationItemListener
import com.freeteaspoon.wppenhacer.xposed.features.media.CallRecording
import com.freeteaspoon.wppenhacer.xposed.features.media.DownloadProfile
import com.freeteaspoon.wppenhacer.xposed.features.media.DownloadViewOnce
import com.freeteaspoon.wppenhacer.xposed.features.media.MediaPreview
import com.freeteaspoon.wppenhacer.xposed.features.media.MediaQuality
import com.freeteaspoon.wppenhacer.xposed.features.media.StatusDownload
import com.freeteaspoon.wppenhacer.xposed.features.others.ActivityController
import com.freeteaspoon.wppenhacer.xposed.features.others.AudioTranscript
import com.freeteaspoon.wppenhacer.xposed.features.others.BackupRestore
import com.freeteaspoon.wppenhacer.xposed.features.others.Channels
import com.freeteaspoon.wppenhacer.xposed.features.others.ChatFilters
import com.freeteaspoon.wppenhacer.xposed.features.others.CopySelectionMessage
import com.freeteaspoon.wppenhacer.xposed.features.others.CopyStatus
import com.freeteaspoon.wppenhacer.xposed.features.others.DebugFeature
import com.freeteaspoon.wppenhacer.xposed.features.others.GoogleTranslate
import com.freeteaspoon.wppenhacer.xposed.features.others.GroupAdmin
import com.freeteaspoon.wppenhacer.xposed.features.others.JumpFirstMessage
import com.freeteaspoon.wppenhacer.xposed.features.others.MenuHome
import com.freeteaspoon.wppenhacer.xposed.features.others.MinorFixes
import com.freeteaspoon.wppenhacer.xposed.features.others.Stickers
import com.freeteaspoon.wppenhacer.xposed.features.others.TextStatusComposer
import com.freeteaspoon.wppenhacer.xposed.features.others.ToastViewer
import com.freeteaspoon.wppenhacer.xposed.features.privacy.AntiWa
import com.freeteaspoon.wppenhacer.xposed.features.privacy.CallPrivacy
import com.freeteaspoon.wppenhacer.xposed.features.privacy.CustomPrivacy
import com.freeteaspoon.wppenhacer.xposed.features.privacy.DndMode
import com.freeteaspoon.wppenhacer.xposed.features.privacy.FreezeLastSeen
import com.freeteaspoon.wppenhacer.xposed.features.privacy.HideChat
import com.freeteaspoon.wppenhacer.xposed.features.privacy.HideSeen
import com.freeteaspoon.wppenhacer.xposed.features.privacy.LockedChatsEnhancer
import com.freeteaspoon.wppenhacer.xposed.features.privacy.TagMessage
import com.freeteaspoon.wppenhacer.xposed.features.privacy.TypingPrivacy
import com.freeteaspoon.wppenhacer.xposed.features.privacy.ViewOnce
import com.freeteaspoon.wppenhacer.xposed.features.providers.ContextMenuActionProvider
import com.freeteaspoon.wppenhacer.xposed.features.providers.MenuStatusProvider

/** Every feature installed by [FeatureLoader], in load order. */
internal object FeatureRegistry {
    val features: List<Class<out Feature>> = listOf(
        DebugFeature::class.java,
        MinorFixes::class.java,
        ContactItemListener::class.java,
        ConversationItemListener::class.java,
        MenuStatusProvider::class.java,
        ShowEditMessage::class.java,
        AntiRevoke::class.java,
        CustomToolbar::class.java,
        CustomView::class.java,
        SeenTick::class.java,
        BubbleColors::class.java,
        CallPrivacy::class.java,
        ActivityController::class.java,
        CustomThemeV2::class.java,
        FloatingBottomBar::class.java,
        ChatLimit::class.java,
        SeparateGroup::class.java,
        ShowOnline::class.java,
        DndMode::class.java,
        FreezeLastSeen::class.java,
        TypingPrivacy::class.java,
        HideChat::class.java,
        HideSeen::class.java,
        HideSeenView::class.java,
        HideHomeViews::class.java,
        HideHomeCamera::class.java,
        TagMessage::class.java,
        HideTabs::class.java,
        IGStatus::class.java,
        MediaQuality::class.java,
        NewChat::class.java,
        CallsButton::class.java,
        Others::class.java,
        PinnedLimit::class.java,
        CustomTime::class.java,
        ShareLimit::class.java,
        StatusDownload::class.java,
        ViewOnce::class.java,
        CallType::class.java,
        MediaPreview::class.java,
        FilterGroups::class.java,
        Tasker::class.java,
        DeleteStatus::class.java,
        DownloadViewOnce::class.java,
        Channels::class.java,
        DownloadProfile::class.java,
        ChatFilters::class.java,
        GroupAdmin::class.java,
        Stickers::class.java,
        CopyStatus::class.java,
        CopySelectionMessage::class.java,
        TextStatusComposer::class.java,
        ToastViewer::class.java,
        MenuHome::class.java,
        AntiWa::class.java,
        CustomPrivacy::class.java,
        AudioTranscript::class.java,
        GoogleTranslate::class.java,
        ContactVerify::class.java,
        LockedChatsEnhancer::class.java,
        CallRecording::class.java,
        BackupRestore::class.java,
        JumpFirstMessage::class.java,
        AboutContactPicker::class.java,
        DefaultEmoji::class.java,
        CaptureDevice::class.java,
        ContextMenuActionProvider::class.java
    )
}
