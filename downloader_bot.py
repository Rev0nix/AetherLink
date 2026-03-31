import os
import requests
import yt_dlp
import instaloader
import logging
logging.basicConfig(level=logging.INFO)
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ApplicationBuilder, CommandHandler, MessageHandler, CallbackQueryHandler, filters, ContextTypes

TOKEN = "8724834546:AAFd-eggxu0hJX_F3ZmN3RYRYRisV3K-MQY"

# START COMMAND WITH BUTTONS
async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    keyboard = [
        [InlineKeyboardButton("YouTube", callback_data='youtube')],
        [InlineKeyboardButton("Instagram", callback_data='instagram')],
        [InlineKeyboardButton("File", callback_data='file')]
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)

    await update.message.reply_text("Choose download type 👇", reply_markup=reply_markup)

# BUTTON CLICK HANDLER
async def button_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query

    try:
        await query.answer()   # answer quickly
    except:
        pass  # ignore error if expired

    choice = query.data
    context.user_data['mode'] = choice

    try:
        await query.edit_message_text(f"Send your {choice} URL 🔗")
    except:
        await query.message.reply_text(f"Send your {choice} URL 🔗")

# MESSAGE HANDLER
async def handle_url(update: Update, context: ContextTypes.DEFAULT_TYPE):
    url = update.message.text
    mode = context.user_data.get('mode')

    if not mode:
        await update.message.reply_text("Please click /start and choose an option first")
        return

    if mode == "youtube":
        await youtube_download(update, url)
    elif mode == "instagram":
        await instagram_download(update, url)
    elif mode == "file":
        await file_download(update, url)

# ---------------- FILE ----------------
async def file_download(update, url):
    msg = await update.message.reply_text("Downloading... ⏳")

    try:
        r = requests.get(url, stream=True)
        filename = url.split("/")[-1] or "file.dat"

        with open(filename, "wb") as f:
            for chunk in r.iter_content(1024):
                f.write(chunk)

        await msg.edit_text("Uploading... 📤")

        with open(filename, "rb") as f:
            await update.message.reply_document(f)

        os.remove(filename)

    except Exception as e:
        await update.message.reply_text(f"Error: {e}")

# ---------------- YOUTUBE ----------------
async def youtube_download(update, url):
    msg = await update.message.reply_text("Downloading YouTube... 🎥")

    try:
        ydl_opts = {'outtmpl': '%(title)s.%(ext)s', 'format': 'best'}

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            filename = ydl.prepare_filename(info)

        await msg.edit_text("Uploading... 📤")

        with open(filename, "rb") as f:
            await update.message.reply_document(f)

        os.remove(filename)

    except Exception as e:
        await update.message.reply_text(f"Error: {e}")

# ---------------- INSTAGRAM ----------------
async def instagram_download(update, url):
    msg = await update.message.reply_text("Downloading Instagram... 📸")

    try:
        ydl_opts = {
            'outtmpl': '%(title)s.%(ext)s',
            'format': 'best'
        }

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            filename = ydl.prepare_filename(info)

        await msg.edit_text("Uploading... 📤")

        with open(filename, "rb") as f:
            await update.message.reply_document(f)

        os.remove(filename)

    except Exception as e:
        await update.message.reply_text(f"Error: {e}")
# ---------------- BOT SETUP ----------------
app = ApplicationBuilder().token(TOKEN).build()

app.add_handler(CommandHandler("start", start))
app.add_handler(CallbackQueryHandler(button_handler))
app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_url))

print("Bot running 🚀")
app.run_polling()

