# Settings parity checklist

Ticked items are wired in `SettingsStore` and the settings screen. Generated from `app/src/main/res/xml/preferences.xml` of Smouldering Durtles (Apache 2.0). Each item becomes a parity test case.

Total preferences: 178

## API and audio download settings

- [ ] `api_key_help` (InfoPreference) Caution
- [ ] `api_key` (EncryptedEditTextPreference) V2 API token
- [ ] `network_rule` (ListPreference) Network access rule — default `DOWNLOAD_WIFI_ONLY`
- [ ] `audio_location` (ListPreference) Audio files location — default `Internal`
- [ ] `auto_download_audio` (SwitchPreferenceCompat) Auto-download audio for upcoming reviews — default `true`
- [x] `enable_background_sync` (SwitchPreferenceCompat) Allow background sync — default `false`
- [x] `sync_on_open` (SwitchPreferenceCompat) Sync when the app is opened — default `false`
- [ ] `web_password` (EncryptedEditTextPreference) Web site password

## Display settings

- [x] `theme` (ListPreference) Light Theme — default `LIGHT`
- [x] `nightTheme` (ListPreference) Dark Theme — default `DARK`
- [ ] `theme_customization` (Preference) Theme Customisation
- [ ] `center_caret` (SwitchPreferenceCompat) Center caret in quiz answer box — default `false`
- [ ] `anki_buttons_height` (SeekBarPreference) Vertical size of the Anki mode buttons — default `50`
- [ ] `quiz_question_view_height` (SeekBarPreference) Fix height of main quiz question view — default `0`
- [ ] `swap_anki_buttons` (SwitchPreferenceCompat) Swap the correct/incorrect Anki mode buttons — default `false`
- [ ] `slide_animations` (SwitchPreferenceCompat) Use slide animations to transition between quiz questions — default `true`
- [ ] `stretch_quiz_question_view` (SwitchPreferenceCompat) Stretch the question view to fill the screen when keyboard input is active — default `false`

## Dashboard settings

- [ ] `show_lesson_review_breakdown` (SwitchPreferenceCompat) Show lesson/review breakdown — default `false`
- [ ] `show_timeline` (SwitchPreferenceCompat) Show 24h review timeline — default `true`
- [ ] `timeline_chart_style` (ListPreference) Timeline chart style — default `SRS_STAGE`
- [ ] `show_waterfall_line` (SwitchPreferenceCompat) Show timeline waterfall line — default `false`
- [ ] `timeline_chart_grid_style` (ListPreference) Timeline chart Y-axis grid style — default `FOR_BARS`
- [ ] `timeline_chart_size` (ListPreference) Timeline chart size — default `24`
- [ ] `timeline_chart_size_shown` (ListPreference) Part of timeline chart shown at once — default `24`
- [ ] `show_srs_breakdown` (SwitchPreferenceCompat) Show SRS breakdown — default `true`
- [ ] `show_level_progression` (SwitchPreferenceCompat) Show level progression bars — default `true`
- [ ] `show_over_level_progression` (SwitchPreferenceCompat) Show level progression bars for levels higher than your own — default `false`
- [ ] `previous_level_progression_limit` (SwitchPreferenceCompat) Limit progression bars for the completed levels to only the most recent levels — default `false`
- [ ] `previous_level_progression_limit_amount` (EditLimitedIntPreference) Number of previous levels to show progression bars for — default `1`
- [ ] `show_post60_progression` (SwitchPreferenceCompat) Show post-60 progression bar — default `false`
- [ ] `show_post60_subsections` (SwitchPreferenceCompat) Show subsections in post-60 progression bar — default `false`
- [ ] `show_post60_reverse` (SwitchPreferenceCompat) Reverse post-60 progression bar — default `false`
- [ ] `show_joyo_progress` (SwitchPreferenceCompat) Show Jōyō progress — default `false`
- [ ] `show_jlpt_progress` (SwitchPreferenceCompat) Show JLPT progress — default `false`
- [ ] `show_recent_unlocks` (SwitchPreferenceCompat) Show recently unlocked items — default `true`
- [ ] `show_critical_condition` (SwitchPreferenceCompat) Show critical condition items — default `true`
- [ ] `show_burned_items` (SwitchPreferenceCompat) Show recently burned items — default `true`

## Subject Info settings

- [ ] `hide_lesson_readings` (SwitchPreferenceCompat) Hide readings in lessons until tapped — default `false`
- [ ] `hide_sentence_translations` (SwitchPreferenceCompat) Hide context sentence translations until tapped — default `true`
- [ ] `show_pitch_info` (SwitchPreferenceCompat) Show pitch information on vocabulary — default `false`
- [ ] `swap_similar_amalgamations` (SwitchPreferenceCompat) Swap 'visually similar' and 'used in' in subject info dump — default `false`
- [ ] `highlight_subject_tags` (SwitchPreferenceCompat) Highlight subject tags in mnemonic texts with colours — default `true`
- [ ] `not_always_hide_wk_mnemonic` (SwitchPreferenceCompat) Always hide WaniKani's own mnemonics — default `true`
- [ ] `always_hide_wk_mnemonic` (SwitchPreferenceCompat) Always hide WaniKani's own mnemonics — default `false`
- [ ] `hide_meaning_mnemonic` (SwitchPreferenceCompat) Hide WaniKani's meaning mnemonic if a user note is present — default `false`
- [ ] `hide_reading_mnemonic` (SwitchPreferenceCompat) Hide WaniKani's reading mnemonic if a user note is present — default `false`
- [ ] `show_legacy` (SwitchPreferenceCompat) Show old names and mnemonics for radicals from before the December 2018 content update — default `false`
- [ ] `meaning_info_dump` (ListPreference) Show info dump after a correct meaning answer — default `ANSWERS_ONLY`
- [ ] `reading_info_dump` (ListPreference) Show info dump after a correct reading answer — default `ANSWERS_ONLY`
- [ ] `meaning_info_dump_incorrect` (ListPreference) Show info dump after an incorrect meaning answer — default `ANSWERS_ONLY`
- [ ] `reading_info_dump_incorrect` (ListPreference) Show info dump after an incorrect reading answer — default `ANSWERS_ONLY`
- [ ] `show_stroke_order` (SwitchPreferenceCompat) Show stroke order diagrams on radicals and kanji — default `false`
- [ ] `stroke_order_animated` (SwitchPreferenceCompat) Animate the stroke order diagrams — default `true`
- [ ] `stroke_order_size` (SeekBarPreference) Size of the stroke order diagrams — default `250`

## Audio settings

- [ ] `autoplay_lesson_presentation` (SwitchPreferenceCompat) Autoplay audio during lesson presentation — default `true`
- [ ] `autoplay_lessons` (SwitchPreferenceCompat) Autoplay audio during lessons — default `true`
- [ ] `autoplay_reviews` (SwitchPreferenceCompat) Autoplay audio during reviews — default `true`
- [ ] `autoplay_anki_reveal` (SwitchPreferenceCompat) Autoplay audio when revealing in Anki mode — default `true`
- [ ] `voice_preference` (ListPreference) Voice preference — default `RANDOM`
- [ ] `use_audio_focus` (SwitchPreferenceCompat) Request audio focus when playing vocab audio — default `true`

## Lesson/review settings

- [x] `max_lesson_session_size` (EditTextPreference) Max. # of lessons per session — default `5`
- [x] `max_review_session_size` (EditTextPreference) Max. # of reviews per session — default `100`
- [x] `max_self_study_session_size` (EditTextPreference) Max. # of self-study items per session — default `100`
- [x] `delay_result_upload` (SwitchPreferenceCompat) Delay processing quiz results — default `false` (ours defaults to true)
- [ ] `enable_lightning_mode` (SwitchPreferenceCompat) Enable lightning mode — default `true`
- [ ] `show_answer_toast` (SwitchPreferenceCompat) Enable a check mark / X animation for a correct / incorrect answer — default `true`
- [ ] `enable_srs_indicator` (SwitchPreferenceCompat) Enable SRS indicator — default `false`
- [ ] `enable_srs_toast` (SwitchPreferenceCompat) Enable SRS up/downgrade toast — default `true`
- [ ] `enable_alternatives_toast` (SwitchPreferenceCompat) Enable alternative readings/meanings toast — default `false`
- [ ] `enable_haptic_feedback_success` (SwitchPreferenceCompat) Enable haptic feedback on correct answer — default `false`
- [ ] `enable_haptic_feedback_failure` (SwitchPreferenceCompat) Enable haptic feedback on incorrect answer — default `false`
- [x] `close_enough_action` (ListPreference) Action for "close enough" answer — default `SILENTLY_ACCEPT`

## Keyboard input settings

- [ ] `keyboard_help` (Preference) Tap here to read some hints for keyboard settings
- [ ] `enable_auto_correct_meaning` (SwitchPreferenceCompat) Allow keyboard auto-correct for meanings — default `false`
- [ ] `enable_auto_correct_reading` (SwitchPreferenceCompat) Allow keyboard auto-correct for readings — default `false`
- [ ] `force_ascii_meaning` (SwitchPreferenceCompat) Force ASCII keyboard for meanings — default `true`
- [ ] `force_ascii_reading` (SwitchPreferenceCompat) Force ASCII keyboard for readings — default `false`
- [ ] `force_visible_password_meaning` (SwitchPreferenceCompat) Force 'visible password' setting for meanings — default `false`
- [ ] `force_visible_password_reading` (SwitchPreferenceCompat) Force 'visible password' setting for readings — default `false`
- [ ] `ime_hint_meaning` (SwitchPreferenceCompat) Set keyboard language hint to English for meanings — default `false`
- [ ] `ime_hint_reading` (SwitchPreferenceCompat) Set keyboard language hint to Japanese for readings — default `false`
- [ ] `enable_no_learning` (SwitchPreferenceCompat) Enable 'no personalized learning' option — default `true`
- [ ] `next_button_delay` (EditTextPreference) Delay in seconds to disable the next/submit button after typing an incorrect answer — default `0`

## Font settings

- [ ] `font_selection` (Preference) Select fonts for quiz questions
- [ ] `font_import` (Preference) Import fonts for quiz questions
- [ ] `max_font_size_lesson` (SeekBarPreference) Max font size of subject title in lesson presentation — default `128`
- [ ] `max_font_size_quiz` (SeekBarPreference) Max font size of subject title on the quiz screen — default `28`
- [ ] `max_font_size_browse` (SeekBarPreference) Max font size of subject title on the browse screen — default `64`
- [ ] `max_font_size_quiz_text` (SeekBarPreference) Max font size of main quiz question text — default `100`
- [ ] `font_size_question_edit` (SeekBarPreference) Font size of answer input text box — default `14`
- [ ] `font_size_subject_info` (SeekBarPreference) Font size of subject info dump — default `14`
- [ ] `font_size_live_subject_table` (SeekBarPreference) Font size of dashboard subject tables — default `14`
- [ ] `font_size_anki_answer` (SeekBarPreference) Font size of Anki mode answers — default `18`

## Notifications and other settings

- [ ] `grant_notification_permission` (Preference) Grant notification permission
- [x] `enable_notifications` (SwitchPreferenceCompat) Enable notifications for new reviews — default `true`
- [x] `notification_update_frequency` (ListPreference) Notification update frequency — default `ONLY_NEW_REVIEWS`
- [x] `notification_priority` (ListPreference) Notification priority — default `DEFAULT`
- [x] `notification_category` (ListPreference) Notification category — default `RECOMMENDATION`
- [ ] `search_engine_1` (TaggedUrlPreference) Search engine 1 — default `https://ja.wiktionary.org/w/index.php?search=%s`
- [ ] `search_engine_2` (TaggedUrlPreference) Search engine 2 — default `https://jisho.org/search/%s`
- [ ] `search_engine_3` (TaggedUrlPreference) Search engine 3
- [ ] `search_engine_4` (TaggedUrlPreference) Search engine 4
- [ ] `search_engine_5` (TaggedUrlPreference) Search engine 5
- [ ] `enable_star_ratings` (SwitchPreferenceCompat) Enable star ratings for subjects — default `false`
- [ ] `show_onyomi_in_katakana` (SwitchPreferenceCompat) Show on'yomi readings in Katakana — default `false`
- [x] `require_onyomi_in_katakana` (SwitchPreferenceCompat) Require on'yomi answers in Katakana — default `false`

## Experimental (temporary) settings

- [ ] `experimental_status` (InfoPreference) Caution
- [ ] `exp_subject_card_layout_search` (ListPreference) Subject card layout in search results — default `NORMAL`
- [ ] `exp_subject_card_layout_grid` (ListPreference) Subject card layout in other places — default `NORMAL`

## Advanced settings

- [ ] `enable_advanced` (SwitchPreferenceCompat) Enable advanced settings — default `false`

### Lesson advanced settings

- [x] `lesson_order` (ListPreference) Order — default `LEVEL_THEN_TYPE`
- [ ] `lesson_order_reversed` (SwitchPreferenceCompat) Reverse order — default `false`
- [ ] `lesson_order_priority` (ListPreference) Priority — default `NONE`
- [ ] `lesson_shuffle_after_selection` (SwitchPreferenceCompat) Shuffle after selecting items — default `false`
- [ ] `lesson_back_to_back` (SwitchPreferenceCompat) Reading/meaning back-to-back — default `false`
- [ ] `lesson_reading_first` (SwitchPreferenceCompat) Reading before meaning — default `false`
- [ ] `lesson_meaning_first` (SwitchPreferenceCompat) Meaning before reading — default `false`
- [ ] `anki_mode_lesson_meaning` (SwitchPreferenceCompat) Anki mode for meanings — default `false`
- [ ] `anki_mode_lesson_reading` (SwitchPreferenceCompat) Anki mode for readings — default `false`
- [ ] `lesson_subject_selection` (InfoPreference) 
- [ ] `lesson_subjects_current` (NumberRangePreference) Current-level items
- [ ] `lesson_subjects_earlier` (NumberRangePreference) Earlier-level items
- [ ] `lesson_subjects_radical` (NumberRangePreference) Radicals
- [ ] `lesson_subjects_kanji` (NumberRangePreference) Kanji
- [ ] `lesson_subjects_vocabulary` (NumberRangePreference) Vocabulary
- [ ] `lesson_subjects_kana_vocabulary` (NumberRangePreference) Kana Vocabulary

### Review advanced settings

- [x] `review_order` (ListPreference) Order — default `SHUFFLE`
- [x] `review_order_reversed` (SwitchPreferenceCompat) Reverse order — default `false`
- [x] `review_order_overdue_first` (SwitchPreferenceCompat) Overdue items first — default `false`
- [x] `review_order_priority` (ListPreference) Priority — default `NONE`
- [ ] `review_shuffle_after_selection` (SwitchPreferenceCompat) Shuffle after selecting items — default `false`
- [x] `review_back_to_back` (SwitchPreferenceCompat) Reading/meaning back-to-back — default `false`
- [x] `review_reading_first` (SwitchPreferenceCompat) Reading before meaning — default `false`
- [x] `review_meaning_first` (SwitchPreferenceCompat) Meaning before reading — default `false`
- [ ] `anki_mode_review_meaning` (SwitchPreferenceCompat) Anki mode for meanings — default `false`
- [ ] `anki_mode_review_reading` (SwitchPreferenceCompat) Anki mode for readings — default `false`
- [ ] `review_randomize_inflections` (SwitchPreferenceCompat) Randomise inflections in review — default `false`
- [ ] `review_subject_selection` (InfoPreference) 
- [ ] `review_subjects_current` (NumberRangePreference) Current-level items
- [ ] `review_subjects_earlier` (NumberRangePreference) Earlier-level items
- [ ] `review_subjects_radical` (NumberRangePreference) Radicals
- [ ] `review_subjects_kanji` (NumberRangePreference) Kanji
- [ ] `review_subjects_vocabulary` (NumberRangePreference) Vocabulary
- [ ] `review_subjects_kana_vocabulary` (NumberRangePreference) Kana Vocabulary

### Self-study advanced settings

- [ ] `self_study_order` (ListPreference) Order — default `SHUFFLE`
- [ ] `self_study_order_reversed` (SwitchPreferenceCompat) Reverse order — default `false`
- [ ] `self_study_order_overdue_first` (SwitchPreferenceCompat) Overdue items first — default `false`
- [ ] `self_study_order_priority` (ListPreference) Priority — default `NONE`
- [ ] `self_study_shuffle_after_selection` (SwitchPreferenceCompat) Shuffle after selecting items — default `false`
- [ ] `self_study_back_to_back` (SwitchPreferenceCompat) Reading/meaning back-to-back — default `false`
- [ ] `self_study_reading_first` (SwitchPreferenceCompat) Reading before meaning — default `false`
- [ ] `self_study_meaning_first` (SwitchPreferenceCompat) Meaning before reading — default `false`
- [ ] `anki_mode_self_study_meaning` (SwitchPreferenceCompat) Anki mode for meanings — default `false`
- [ ] `anki_mode_self_study_reading` (SwitchPreferenceCompat) Anki mode for readings — default `false`
- [ ] `self_study_randomize_inflections` (SwitchPreferenceCompat) Randomize inflections in self-study — default `false`
- [ ] `self_study_subject_selection` (InfoPreference) 
- [ ] `self_study_subjects_current` (NumberRangePreference) Current-level items
- [ ] `self_study_subjects_earlier` (NumberRangePreference) Earlier-level items
- [ ] `self_study_subjects_radical` (NumberRangePreference) Radicals
- [ ] `self_study_subjects_kanji` (NumberRangePreference) Kanji
- [ ] `self_study_subjects_vocabulary` (NumberRangePreference) Vocabulary
- [ ] `self_study_subjects_kana_vocabulary` (NumberRangePreference) Kana Vocabulary

### Other advanced settings

- [ ] `overdue_threshold` (EditTextPreference) Overdue threshold percentage — default `20`
- [ ] `kanji_mode_onkun` (SwitchPreferenceCompat) Quiz on/kun'yomi separately — default `false`
- [ ] `undo_button` (ListPreference) Special button 1 behavior (top right) — default `DISABLE`
- [ ] `ignore_button` (ListPreference) Special button 2 behavior (bottom left) — default `DISABLE`
- [ ] `special_button_3` (ListPreference) Special button 3 behavior (top left) — default `DISABLE`
- [ ] `dont_know_button` (ListPreference) Don't know button behavior — default `ONLY_IF_IGNORE_HIDDEN`
- [ ] `shake_on_matching_kanji` (SwitchPreferenceCompat) Soft-reject on single-kanji vocabulary — default `true`
- [ ] `full_session_log` (SwitchPreferenceCompat) Show full session log — default `false`
- [ ] `indicate_kanji_reading_type` (SwitchPreferenceCompat) Indicate type of reading for kanji quiz — default `false`

## Other actions

- [ ] `about_this_app` (Preference) About this app
- [ ] `support_and_feedback` (Preference) Support and feedback
- [ ] `data_import_export` (Preference) Data import / export
- [ ] `reset_tutorials` (Preference) Reset UI confirmations and tutorials
- [ ] `reset_database` (Preference) Reset database
- [x] `backup_settings` (Preference) Backup your settings
- [ ] `restore_settings` (Preference) Restore your settings
