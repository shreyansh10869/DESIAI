import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const PORT = 3000;

function getServerGeminiKey() {
  if (process.env.GEMINI_API_KEY) return process.env.GEMINI_API_KEY.trim();
  try {
    const devEnv = JSON.parse(fs.readFileSync('/app/.dev.env.json', 'utf8'));
    if (devEnv && devEnv.GEMINI_API_KEY) return devEnv.GEMINI_API_KEY.trim();
  } catch {}
  return '';
}

function sanitizeCjk(text) {
  return String(text || '').replace(/[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]/g, '');
}

const HTML_PAGE = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Auralis AI</title>
  <meta name="description" content="Auralis AI — Minimalist, Clean ChatGPT-Style Interface with Fast and Deep Think Modes" />
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    html, body {
      height: 100%;
      width: 100%;
      background-color: #0A0A0A;
      color: #ECECEC;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      overflow: hidden;
    }
    .app-shell {
      display: flex;
      flex-direction: column;
      height: 100vh;
      max-width: 768px;
      margin: 0 auto;
      background-color: #0A0A0A;
    }
    /* Minimal Top Bar */
    .top-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      background-color: #0A0A0A;
      flex-shrink: 0;
    }
    .brand-title {
      font-size: 18px;
      font-weight: 600;
      color: #ECECEC;
      letter-spacing: -0.01em;
    }
    .mode-pill {
      display: inline-flex;
      background-color: #171717;
      border-radius: 20px;
      padding: 3px;
      gap: 2px;
    }
    .mode-btn {
      border: none;
      background: transparent;
      color: #9B9B9B;
      font-size: 12.5px;
      font-weight: 500;
      padding: 6px 13px;
      border-radius: 17px;
      cursor: pointer;
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .mode-btn.active {
      background-color: #2B2B2B;
      color: #ECECEC;
      font-weight: 600;
    }
    .icon-btn {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: none;
      background: transparent;
      color: #9B9B9B;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .icon-btn:hover {
      background-color: #171717;
      color: #ECECEC;
    }
    /* Chat Stream */
    .chat-container {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      scroll-behavior: smooth;
    }
    .empty-state {
      margin: auto;
      text-align: center;
      color: #ECECEC;
      font-size: 22px;
      font-weight: 600;
      letter-spacing: -0.01em;
    }
    .msg-row {
      display: flex;
      flex-direction: column;
      width: 100%;
    }
    .msg-row.user {
      align-items: flex-end;
    }
    .msg-row.ai {
      align-items: flex-start;
    }
    .user-bubble {
      max-width: 82%;
      background-color: #1F1F1F;
      color: #ECECEC;
      padding: 11px 16px;
      border-radius: 20px;
      font-size: 15.5px;
      line-height: 1.45;
      white-space: pre-wrap;
      word-break: break-word;
    }
    .user-image {
      max-width: 180px;
      max-height: 180px;
      border-radius: 14px;
      margin-bottom: 6px;
      object-fit: cover;
    }
    .ai-text {
      width: 100%;
      color: #ECECEC;
      font-size: 15.5px;
      line-height: 1.55;
      word-break: break-word;
    }
    .ai-text p { margin-bottom: 10px; }
    .ai-text p:last-child { margin-bottom: 0; }
    .ai-text pre {
      background-color: #141414;
      padding: 12px;
      border-radius: 10px;
      overflow-x: auto;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 13px;
      line-height: 1.45;
      color: #E5E5E5;
      margin: 8px 0;
    }
    .ai-text code {
      background-color: #1E1E1E;
      padding: 2px 5px;
      border-radius: 5px;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 13.5px;
    }
    .ai-text pre code {
      background: transparent;
      padding: 0;
    }
    /* Minimal 3-Dot Typing Capsule */
    .typing-capsule {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      background-color: #171717;
      padding: 10px 12px;
      border-radius: 18px;
    }
    .typing-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #D4D4D4;
      animation: pulseDot 1.1s infinite ease-in-out;
    }
    .typing-dot:nth-child(2) { animation-delay: 0.15s; }
    .typing-dot:nth-child(3) { animation-delay: 0.30s; }
    @keyframes pulseDot {
      0%, 100% { opacity: 0.3; transform: scale(0.85); }
      50% { opacity: 1; transform: scale(1.05); }
    }
    .msg-actions {
      display: flex;
      gap: 4px;
      margin-top: 6px;
    }
    .action-icon {
      width: 28px;
      height: 28px;
      border: none;
      background: transparent;
      color: #6E6E6E;
      border-radius: 6px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .action-icon:hover { color: #ECECEC; background-color: #171717; }
    /* Attachment Preview */
    .attach-preview-bar {
      display: none;
      padding: 6px 16px;
      align-items: center;
    }
    .attach-preview-bar.visible { display: flex; }
    .attach-thumb-wrap {
      position: relative;
      width: 52px;
      height: 52px;
      border-radius: 12px;
      overflow: hidden;
    }
    .attach-thumb-wrap img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .remove-attach {
      position: absolute;
      top: 3px;
      right: 3px;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      border: none;
      background: rgba(0,0,0,0.75);
      color: #FFF;
      font-size: 11px;
      cursor: pointer;
    }
    /* Floating Input Bar */
    .bottom-dock-wrap {
      padding: 8px 12px 14px 12px;
      background-color: #0A0A0A;
      flex-shrink: 0;
    }
    .floating-input-bar {
      display: flex;
      align-items: center;
      background-color: #171717;
      border-radius: 26px;
      padding: 6px 8px;
      gap: 10px;
    }
    .circle-btn {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      border: none;
      background-color: #242424;
      color: #ECECEC;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      flex-shrink: 0;
      transition: transform 0.12s ease, background-color 0.15s ease;
    }
    .circle-btn:active { transform: scale(0.88); }
    .circle-btn.send-active {
      background-color: #FFFFFF;
      color: #0A0A0A;
    }
    .prompt-input {
      flex: 1;
      background: transparent;
      border: none;
      outline: none;
      color: #ECECEC;
      font-size: 15.5px;
      line-height: 21px;
      resize: none;
      max-height: 110px;
      font-family: inherit;
    }
    .prompt-input::placeholder { color: #6E6E6E; }
    /* Settings Modal */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.7);
      display: none;
      align-items: center;
      justify-content: center;
      padding: 16px;
      z-index: 50;
    }
    .modal-backdrop.open { display: flex; }
    .modal-card {
      background-color: #171717;
      border-radius: 20px;
      width: 100%;
      max-width: 400px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .modal-title {
      font-size: 18px;
      font-weight: 600;
      color: #ECECEC;
    }
    .field-label {
      font-size: 12px;
      color: #9B9B9B;
      margin-bottom: 4px;
      display: block;
    }
    .field-input {
      width: 100%;
      padding: 10px 12px;
      border-radius: 10px;
      border: 1px solid #2B2B2B;
      background-color: #0A0A0A;
      color: #ECECEC;
      font-size: 14px;
      outline: none;
    }
    .modal-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 6px;
    }
    .btn-text {
      background: transparent;
      border: none;
      color: #9B9B9B;
      font-size: 14px;
      cursor: pointer;
      padding: 8px 10px;
    }
    .btn-danger { color: #FF6B6B; }
    .btn-primary {
      background-color: #FFFFFF;
      color: #0A0A0A;
      border: none;
      border-radius: 18px;
      padding: 8px 18px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
    }
    .apk-link {
      display: inline-block;
      text-decoration: none;
      color: #9B9B9B;
      font-size: 12.5px;
      padding-top: 4px;
    }
    .apk-link:hover { color: #ECECEC; }
  </style>
</head>
<body>
  <div class="app-shell">
    <header class="top-bar">
      <div class="brand-title">Auralis AI</div>
      <div class="mode-pill">
        <button class="mode-btn active" id="modeFastBtn" type="button">Fast</button>
        <button class="mode-btn" id="modeDeepBtn" type="button">Deep Think</button>
      </div>
      <button class="icon-btn" id="openSettingsBtn" type="button" aria-label="Settings">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
      </button>
    </header>

    <main class="chat-container" id="chatContainer">
      <div class="empty-state" id="emptyState">What can I help with?</div>
    </main>

    <div class="attach-preview-bar" id="attachPreviewBar">
      <div class="attach-thumb-wrap">
        <img id="attachThumbImg" src="" alt="Attached" />
        <button class="remove-attach" id="removeAttachBtn" type="button">✕</button>
      </div>
    </div>

    <div class="bottom-dock-wrap">
      <div class="floating-input-bar">
        <input type="file" id="fileInput" accept="image/*" style="display:none" />
        <button class="circle-btn" id="attachBtn" type="button" aria-label="Add attachment">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </button>
        <textarea
          class="prompt-input"
          id="promptInput"
          rows="1"
          placeholder="Message"
        ></textarea>
        <button class="circle-btn" id="sendOrMicBtn" type="button" aria-label="Send">
          <span id="actionIconWrap"></span>
        </button>
      </div>
    </div>
  </div>

  <div class="modal-backdrop" id="settingsModal">
    <div class="modal-card">
      <div class="modal-title">Settings</div>
      <div>
        <label class="field-label">Gemini API Key</label>
        <input class="field-input" id="geminiKeyField" type="password" placeholder="AIzaSy..." />
      </div>
      <div>
        <label class="field-label">Groq API Key (Optional)</label>
        <input class="field-input" id="groqKeyField" type="password" placeholder="gsk_..." />
      </div>
      <a class="apk-link" href="/download/app-debug.apk" download="Auralis-AI-debug.apk">Download Android APK (app-debug.apk)</a>
      <div class="modal-actions">
        <button class="btn-text btn-danger" id="clearChatBtn" type="button">Clear conversation</button>
        <div>
          <button class="btn-text" id="closeSettingsBtn" type="button">Cancel</button>
          <button class="btn-primary" id="saveSettingsBtn" type="button">Save</button>
        </div>
      </div>
    </div>
  </div>

  <script>
    const MIC_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path><path d="M19 10v2a7 7 0 0 1-14 0v-2"></path><line x1="12" y1="19" x2="12" y2="23"></line><line x1="8" y1="23" x2="16" y2="23"></line></svg>';
    const SEND_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>';

    let engineMode = localStorage.getItem('auralis_mode') || 'FAST_MODE';
    let attachedImageBase64 = null;
    let isSynthesizing = false;
    let history = [];

    const chatContainer = document.getElementById('chatContainer');
    const emptyState = document.getElementById('emptyState');
    const promptInput = document.getElementById('promptInput');
    const sendOrMicBtn = document.getElementById('sendOrMicBtn');
    const actionIconWrap = document.getElementById('actionIconWrap');
    const modeFastBtn = document.getElementById('modeFastBtn');
    const modeDeepBtn = document.getElementById('modeDeepBtn');
    const attachBtn = document.getElementById('attachBtn');
    const fileInput = document.getElementById('fileInput');
    const attachPreviewBar = document.getElementById('attachPreviewBar');
    const attachThumbImg = document.getElementById('attachThumbImg');
    const removeAttachBtn = document.getElementById('removeAttachBtn');
    const settingsModal = document.getElementById('settingsModal');
    const openSettingsBtn = document.getElementById('openSettingsBtn');
    const closeSettingsBtn = document.getElementById('closeSettingsBtn');
    const saveSettingsBtn = document.getElementById('saveSettingsBtn');
    const clearChatBtn = document.getElementById('clearChatBtn');
    const geminiKeyField = document.getElementById('geminiKeyField');
    const groqKeyField = document.getElementById('groqKeyField');

    function syncModeUI() {
      modeFastBtn.classList.toggle('active', engineMode === 'FAST_MODE');
      modeDeepBtn.classList.toggle('active', engineMode === 'DEEP_THINK');
    }
    syncModeUI();

    modeFastBtn.onclick = () => { engineMode = 'FAST_MODE'; localStorage.setItem('auralis_mode', engineMode); syncModeUI(); };
    modeDeepBtn.onclick = () => { engineMode = 'DEEP_THINK'; localStorage.setItem('auralis_mode', engineMode); syncModeUI(); };

    function syncActionBtn() {
      const hasInput = promptInput.value.trim().length > 0 || Boolean(attachedImageBase64);
      sendOrMicBtn.classList.toggle('send-active', hasInput);
      actionIconWrap.innerHTML = hasInput ? SEND_SVG : MIC_SVG;
    }
    syncActionBtn();

    promptInput.addEventListener('input', () => {
      promptInput.style.height = 'auto';
      promptInput.style.height = Math.min(promptInput.scrollHeight, 110) + 'px';
      syncActionBtn();
    });

    promptInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (promptInput.value.trim() || attachedImageBase64) sendMessage();
      }
    });

    attachBtn.onclick = () => fileInput.click();
    fileInput.onchange = (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        attachedImageBase64 = String(reader.result);
        attachThumbImg.src = attachedImageBase64;
        attachPreviewBar.classList.add('visible');
        syncActionBtn();
      };
      reader.readAsDataURL(file);
    };

    removeAttachBtn.onclick = () => {
      attachedImageBase64 = null;
      fileInput.value = '';
      attachPreviewBar.classList.remove('visible');
      syncActionBtn();
    };

    openSettingsBtn.onclick = () => {
      geminiKeyField.value = localStorage.getItem('auralis_gemini_key') || '';
      groqKeyField.value = localStorage.getItem('auralis_groq_key') || '';
      settingsModal.classList.add('open');
    };
    closeSettingsBtn.onclick = () => settingsModal.classList.remove('open');
    saveSettingsBtn.onclick = () => {
      localStorage.setItem('auralis_gemini_key', geminiKeyField.value.trim());
      localStorage.setItem('auralis_groq_key', groqKeyField.value.trim());
      settingsModal.classList.remove('open');
    };
    clearChatBtn.onclick = () => {
      history = [];
      chatContainer.innerHTML = '<div class="empty-state" id="emptyState">What can I help with?</div>';
      settingsModal.classList.remove('open');
    };

    function escapeHtml(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function formatMarkdown(text) {
      const parts = text.split(/(\`\`\`[\\s\\S]*?\`\`\`)/g);
      return parts.map(part => {
        if (part.startsWith('\`\`\`') && part.endsWith('\`\`\`')) {
          const lines = part.slice(3, -3).trim().split('\\n');
          const code = lines.length > 1 && /^[a-zA-Z0-9_-]+$/.test(lines[0].trim())
            ? lines.slice(1).join('\\n')
            : lines.join('\\n');
          return '<pre><code>' + escapeHtml(code) + '</code></pre>';
        }
        let html = escapeHtml(part);
        html = html.replace(/\\*\\*(.+?)\\*\\*/g, '<strong>$1</strong>');
        html = html.replace(/\`([^\`]+)\`/g, '<code>$1</code>');
        return html.split('\\n\\n').map(p => '<p>' + p.replace(/\\n/g, '<br/>') + '</p>').join('');
      }).join('');
    }

    async function sendMessage() {
      if (isSynthesizing) return;
      const text = promptInput.value.trim();
      const image = attachedImageBase64;
      if (!text && !image) return;

      const emptyEl = document.getElementById('emptyState');
      if (emptyEl) emptyEl.remove();

      const userRow = document.createElement('div');
      userRow.className = 'msg-row user';
      if (image) {
        const imgEl = document.createElement('img');
        imgEl.className = 'user-image';
        imgEl.src = image;
        userRow.appendChild(imgEl);
      }
      const userBubble = document.createElement('div');
      userBubble.className = 'user-bubble';
      userBubble.textContent = text || 'Attached image';
      userRow.appendChild(userBubble);
      chatContainer.appendChild(userRow);

      promptInput.value = '';
      promptInput.style.height = 'auto';
      attachedImageBase64 = null;
      fileInput.value = '';
      attachPreviewBar.classList.remove('visible');
      syncActionBtn();

      const aiRow = document.createElement('div');
      aiRow.className = 'msg-row ai';
      const capsule = document.createElement('div');
      capsule.className = 'typing-capsule';
      capsule.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
      aiRow.appendChild(capsule);
      chatContainer.appendChild(aiRow);
      chatContainer.scrollTop = chatContainer.scrollHeight;

      isSynthesizing = true;
      try {
        const res = await fetch('/api/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            prompt: text,
            imageBase64: image,
            engineMode,
            history: history.slice(-10),
            geminiKey: localStorage.getItem('auralis_gemini_key') || '',
            groqKey: localStorage.getItem('auralis_groq_key') || ''
          })
        });
        const data = await res.json();
        const replyText = (data.reply || data.error || 'Unable to reach AI service.').trim();

        aiRow.innerHTML = '';
        const aiTextEl = document.createElement('div');
        aiTextEl.className = 'ai-text';

        const words = replyText.split(/(?<=\\s)/);
        let acc = '';
        const step = Math.max(2, Math.min(8, Math.floor(words.length / 28)));
        for (let i = 0; i < words.length; i += step) {
          acc += words.slice(i, i + step).join('');
          aiTextEl.innerHTML = formatMarkdown(acc);
          if (!aiTextEl.parentNode) aiRow.appendChild(aiTextEl);
          chatContainer.scrollTop = chatContainer.scrollHeight;
          await new Promise(r => setTimeout(r, 14));
        }

        if (data.reply) {
          history.push({ role: 'user', text: text || 'Attached image' });
          history.push({ role: 'model', text: replyText });
        }
      } catch (err) {
        aiRow.innerHTML = '<div class="ai-text"><p>Unable to reach AI service. Please check your connection or API key.</p></div>';
      } finally {
        isSynthesizing = false;
        chatContainer.scrollTop = chatContainer.scrollHeight;
      }
    }

    sendOrMicBtn.onclick = () => {
      if (promptInput.value.trim() || attachedImageBase64) {
        sendMessage();
      } else {
        promptInput.focus();
      }
    };
  </script>
</body>
</html>`;

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || '/', `http://localhost:${PORT}`);

  if (req.method === 'GET' && url.pathname === '/download/app-debug.apk') {
    const apkPath = path.resolve('/app/applet/app/build/outputs/apk/debug/app-debug.apk');
    if (fs.existsSync(apkPath)) {
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="Auralis-AI-debug.apk"'
      });
      fs.createReadStream(apkPath).pipe(res);
      return;
    }
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('APK is building, please try again in a moment.');
    return;
  }

  if (req.method === 'POST' && url.pathname === '/api/chat') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const prompt = sanitizeCjk(payload.prompt || '');
        const engineMode = payload.engineMode || 'FAST_MODE';
        const history = Array.isArray(payload.history) ? payload.history : [];
        const geminiKey = (payload.geminiKey || getServerGeminiKey()).trim();
        const groqKey = (payload.groqKey || process.env.GROQ_API_KEY || '').trim();
        const imageBase64 = payload.imageBase64 || null;

        if (!geminiKey && !groqKey) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Please add your API key in Settings to start chatting.' }));
          return;
        }

        const systemInstruction = engineMode === 'DEEP_THINK'
          ? 'You are Auralis AI, a helpful, clear, and thoughtful assistant. Think carefully step-by-step and provide direct, natural responses without unnecessary boilerplate tables or filler. Never output Chinese characters.'
          : 'You are Auralis AI, a fast, helpful, and concise assistant. Respond naturally and directly like a conversational partner without unnecessary boilerplate tables or filler. Never output Chinese characters.';

        if (engineMode === 'DEEP_THINK' && groqKey && !imageBase64) {
          try {
            const groqResp = await fetch('https://api.groq.com/openai/v1/chat/completions', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${groqKey}`
              },
              body: JSON.stringify({
                model: 'openai/gpt-oss-120b',
                messages: [
                  { role: 'system', content: systemInstruction },
                  ...history.map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: sanitizeCjk(m.text) })),
                  { role: 'user', content: prompt }
                ],
                temperature: 0.5,
                max_tokens: 2048
              })
            });
            if (groqResp.ok) {
              const groqData = await groqResp.json();
              const text = sanitizeCjk(groqData?.choices?.[0]?.message?.content || '').trim();
              if (text) {
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ reply: text }));
                return;
              }
            }
          } catch {}
        }

        if (geminiKey) {
          const models = ['gemini-3.1-flash-lite-preview', 'gemini-2.5-flash'];
          let lastStatus = 500;
          for (const model of models) {
            try {
              const contents = [];
              for (const h of history) {
                contents.push({
                  role: h.role === 'user' ? 'user' : 'model',
                  parts: [{ text: sanitizeCjk(h.text) }]
                });
              }
              const userParts = [];
              if (imageBase64 && imageBase64.includes('base64,')) {
                const [meta, b64] = imageBase64.split('base64,');
                const mimeMatch = meta.match(/data:([^;]+);/);
                userParts.push({
                  inline_data: {
                    mime_type: mimeMatch ? mimeMatch[1] : 'image/jpeg',
                    data: b64
                  }
                });
              }
              userParts.push({ text: prompt || 'Describe and analyze this image.' });
              contents.push({ role: 'user', parts: userParts });

              const gResp = await fetch(
                `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(geminiKey)}`,
                {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({
                    system_instruction: { parts: [{ text: systemInstruction }] },
                    contents,
                    generationConfig: {
                      temperature: engineMode === 'DEEP_THINK' ? 0.4 : 0.7,
                      maxOutputTokens: 2048
                    }
                  })
                }
              );
              lastStatus = gResp.status;
              if (!gResp.ok) continue;
              const gData = await gResp.json();
              const parts = gData?.candidates?.[0]?.content?.parts || [];
              const text = sanitizeCjk(parts.map(p => p.text || '').join('')).trim();
              if (text) {
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ reply: text }));
                return;
              }
            } catch {}
          }
          const msg = (lastStatus === 400 || lastStatus === 403)
            ? 'Invalid Gemini API key. Please check Settings.'
            : `Unable to reach AI service (HTTP ${lastStatus}).`;
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: msg }));
          return;
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Unable to reach AI service. Please check your API key in Settings.' }));
      } catch {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Unable to process request.' }));
      }
    });
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(HTML_PAGE);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Auralis AI server listening on http://localhost:' + PORT);
});
