import asyncio
from telethon import TelegramClient
import os
import subprocess

def get_git_commit_info():
    commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode()
    commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode()
    commit_hash = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%H']).decode()
    commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode()
    return commit_author, commit_message, commit_hash, commit_hash_short

api_id = int(os.getenv("API_ID"))
api_hash = os.getenv("API_HASH")
bot_token = os.getenv("BOT_TOKEN")
group_id = int(os.getenv("CHAT_ID"))
apk_path = os.getenv("APK_PATH")
topic_id = int(os.getenv("TOPIC_ID"))

commit_author, commit_message, commit_hash, commit_hash_short = get_git_commit_info()

session_file = "bot_session.session"
if os.path.exists(session_file):
    os.remove(session_file)

def human_readable_size(size, decimal_places=2):
    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if size < 1024.0:
            break
        size /= 1024.0
    return f"{size:.{decimal_places}f} {unit}"

async def progress(current, total):
    pct = (current / total) * 100
    print(f"{pct:.2f}% - {human_readable_size(current)}/{human_readable_size(total)}", end="\r")

async def send_file(client, file_path):
    if not os.path.exists(file_path):
        print("File not found:", file_path)
        return

    caption = (
        f"**Commit by:** {commit_author}\n"
        f"**Commit message:** {commit_message}\n"
        f"**Commit hash:** #{commit_hash_short}\n"
        f"**Version:** Android >= 8"
    )

    await client.send_file(
        entity=group_id,
        file=file_path,
        caption=caption,
        parse_mode="markdown",
        progress_callback=progress,
        reply_to=topic_id
    )

async def main():
    client = TelegramClient("bot_session", api_id, api_hash)

    await client.start(bot_token=bot_token)

    try:
        await send_file(client, apk_path)
        print("\nFile sent successfully")
    except Exception as e:
        print("\nFailed:", e)
    finally:
        await client.disconnect()

if __name__ == "__main__":
    asyncio.run(main())
