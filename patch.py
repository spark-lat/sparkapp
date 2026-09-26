import os

# 1. Патчим ProfileActivity.java
profile_path = 'TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java'
if os.path.exists(profile_path):
    with open(profile_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Добавляем ID константы
    if 'osint_search_profile' not in content:
        content = content.replace(
            'public class ProfileActivity extends BaseFragment',
            'public class ProfileActivity extends BaseFragment {\n    private final static int osint_search_profile = 100501;'
        )
        
        # Добавляем кнопку в меню
        hook_menu = 'otherItem.addSubItem('
        idx_menu = content.find(hook_menu)
        if idx_menu != -1:
            inject_menu = 'if (user_id != 0) { otherItem.addSubItem(osint_search_profile, R.drawable.msg_search, "Пробить (OSINT)"); }\n        '
            content = content[:idx_menu] + inject_menu + content[idx_menu:]

        # Добавляем обработку клика
        hook_click = 'public void onItemClick(int id) {'
        idx_click = content.find(hook_click)
        if idx_click != -1:
            inject_click = '''public void onItemClick(int id) {
        if (id == osint_search_profile) {
            org.telegram.tgnet.TLRPC.User u = getMessagesController().getUser(user_id);
            OsintHelper.searchByPhone(this, u != null && u.phone != null ? u.phone : "");
            return;
        }'''
            content = content.replace(hook_click, inject_click, 1)

        with open(profile_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("ProfileActivity successfully patched!")
