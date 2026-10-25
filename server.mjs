import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const PORT = 3000;

const DEFAULT_MODELS_CONFIG = {
  fast_primary: 'gemini-2.5-flash',
  fast_fallback: 'gemini-1.5-flash',
  fast_active_lite: 'gemini-3.1-flash-lite',
  deep_think_chain: [
    'llama-3.3-70b-versatile'
  ],
  deep_think_gemini_fallback: 'gemini-2.5-flash'
};

function loadModelsConfig() {
  const candidates = [
    path.resolve('/app/applet/assets/models_config.json'),
    path.resolve('assets/models_config.json'),
    path.resolve('/app/applet/app/src/main/assets/models_config.json')
  ];
  for (const filePath of candidates) {
    try {
      if (fs.existsSync(filePath)) {
        const parsed = JSON.parse(fs.readFileSync(filePath, 'utf8'));
        return {
          fast_primary: parsed.fast_primary || DEFAULT_MODELS_CONFIG.fast_primary,
          fast_fallback: parsed.fast_fallback || DEFAULT_MODELS_CONFIG.fast_fallback,
          fast_active_lite: parsed.fast_active_lite || DEFAULT_MODELS_CONFIG.fast_active_lite,
          deep_think_chain: Array.isArray(parsed.deep_think_chain) && parsed.deep_think_chain.length > 0
            ? parsed.deep_think_chain
            : DEFAULT_MODELS_CONFIG.deep_think_chain,
          deep_think_gemini_fallback:
            parsed.deep_think_gemini_fallback || DEFAULT_MODELS_CONFIG.deep_think_gemini_fallback
        };
      }
    } catch {}
  }
  return DEFAULT_MODELS_CONFIG;
}

function resolveServerSecret(keyName) {
  if (process.env[keyName] && process.env[keyName].trim()) {
    return process.env[keyName].trim();
  }
  try {
    const devEnv = JSON.parse(fs.readFileSync('/app/.dev.env.json', 'utf8'));
    if (devEnv && typeof devEnv[keyName] === 'string' && devEnv[keyName].trim()) {
      return devEnv[keyName].trim();
    }
  } catch {}
  for (const envFile of ['/app/applet/.env', '/app/applet/local.properties']) {
    try {
      if (fs.existsSync(envFile)) {
        const lines = fs.readFileSync(envFile, 'utf8').split('\n');
        for (const line of lines) {
          const trimmed = line.trim();
          if (trimmed.startsWith(keyName + '=')) {
            const val = trimmed.slice(keyName.length + 1).trim().replace(/^["']|["']$/g, '');
            if (val) return val;
          }
        }
      }
    } catch {}
  }
  return '';
}

function sanitizeCjk(text) {
  return String(text || '').replace(/[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]/g, '');
}

const unavailableModelUntil = new Map();

function isModelTemporarilyUnavailable(modelName) {
  const exp = unavailableModelUntil.get(modelName);
  if (!exp) return false;
  if (Date.now() > exp) {
    unavailableModelUntil.delete(modelName);
    return false;
  }
  return true;
}

function markModelTemporarilyUnavailable(modelName, ttlMs = 600000) {
  unavailableModelUntil.set(modelName, Date.now() + ttlMs);
}

const HTML_PAGE = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Auralis AI</title>
  <meta name="description" content="Auralis AI — Authentic ChatGPT-Style Interface with Fast & Deep Think Modes" />
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
    .app-layout {
      display: flex;
      height: 100vh;
      width: 100vw;
      background-color: #0A0A0A;
      position: relative;
    }
    /* Slide-out Left Navigation Drawer */
    .drawer-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      z-index: 40;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
    }
    .drawer-backdrop.open {
      opacity: 1;
      pointer-events: auto;
    }
    .sidebar-drawer {
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      width: 290px;
      background-color: #121212;
      border-right: 1px solid #1F1F1F;
      z-index: 45;
      display: flex;
      flex-direction: column;
      padding: 16px 14px;
      transform: translateX(-100%);
      transition: transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
    }
    .sidebar-drawer.open {
      transform: translateX(0);
    }
    .drawer-header {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 4px 6px 14px 6px;
    }
    .drawer-logo {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: #1F1F1F;
      border: 1px solid #2E2E2E;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ECECEC;
      flex-shrink: 0;
    }
    .drawer-title-group {
      display: flex;
      flex-direction: column;
    }
    .drawer-title {
      font-size: 16.5px;
      font-weight: 600;
      color: #ECECEC;
      letter-spacing: -0.01em;
    }
    .drawer-subtitle {
      font-size: 11.5px;
      color: #7A7A7A;
    }
    .new-chat-pill {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 11px 16px;
      border-radius: 24px;
      background-color: #1E1E1E;
      border: 1px solid #2E2E2E;
      color: #ECECEC;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 0.15s ease;
    }
    .new-chat-pill:hover {
      background-color: #262626;
    }
    .drawer-section-label {
      font-size: 11.5px;
      font-weight: 500;
      color: #7A7A7A;
      padding: 16px 8px 6px 8px;
    }
    .history-list {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .history-empty {
      font-size: 12.5px;
      color: #6E6E6E;
      padding: 10px 8px;
    }
    .history-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 9px 10px;
      border-radius: 12px;
      cursor: pointer;
      color: #B8B8B8;
      font-size: 13.5px;
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .history-item:hover, .history-item.active {
      background-color: #212121;
      color: #ECECEC;
    }
    .history-item-title {
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-right: 8px;
    }
    .history-delete-btn {
      border: none;
      background: transparent;
      color: #7A7A7A;
      cursor: pointer;
      padding: 2px 4px;
      border-radius: 6px;
      font-size: 13px;
    }
    .history-delete-btn:hover {
      color: #ECECEC;
      background: #2E2E2E;
    }
    .drawer-footer {
      border-top: 1px solid #222222;
      padding-top: 12px;
      margin-top: 8px;
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      padding: 10px 6px 4px 6px;
      border-radius: 12px;
    }
    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background-color: #262626;
      color: #ECECEC;
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .user-meta {
      flex: 1;
      min-width: 0;
    }
    .user-name {
      font-size: 13.5px;
      font-weight: 500;
      color: #ECECEC;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .user-email {
      font-size: 11.5px;
      color: #7A7A7A;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    /* Main Chat Shell */
    .app-shell {
      flex: 1;
      display: flex;
      flex-direction: column;
      height: 100vh;
      max-width: 768px;
      margin: 0 auto;
      background-color: #0A0A0A;
      width: 100%;
    }
    /* Authentic ChatGPT Top Bar */
    .top-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      background-color: #0A0A0A;
      flex-shrink: 0;
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
      color: #ECECEC;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.15s ease;
    }
    .icon-btn:hover {
      background-color: #171717;
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
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      text-align: center;
    }
    .empty-spark-badge {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background-color: #171717;
      border: 1px solid #282828;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ECECEC;
    }
    .empty-greeting {
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
      border-radius: 16px;
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
    /* Attachment Preview */
    .attach-preview-bar {
      display: none;
      padding: 6px 16px;
      align-items: center;
    }
    .attach-preview-bar.visible { display: flex; }
    .attach-thumb-wrap {
      position: relative;
      width: 54px;
      height: 54px;
      border-radius: 14px;
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
    /* ChatGPT Floating Bottom Dock (28px radius, #1E1E1E, 1px #2E2E2E) */
    .bottom-dock-wrap {
      padding: 8px 12px 14px 12px;
      background-color: #0A0A0A;
      flex-shrink: 0;
    }
    .floating-input-bar {
      display: flex;
      align-items: center;
      background-color: #1E1E1E;
      border: 1px solid #2E2E2E;
      border-radius: 28px;
      padding: 6px 8px;
      gap: 10px;
    }
    .circle-btn {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      border: none;
      background-color: #2A2A2A;
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
    .settings-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.65);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 60;
      padding: 16px;
    }
    .settings-backdrop.open { display: flex; }
    .settings-card {
      width: 100%;
      max-width: 360px;
      background: #161616;
      border: 1px solid #282828;
      border-radius: 20px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .settings-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 16px;
      font-weight: 600;
      color: #ECECEC;
    }
    .settings-row {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 10px 12px;
      background: #1E1E1E;
      border-radius: 12px;
      font-size: 13px;
      color: #9B9B9B;
    }
    .settings-row strong {
      color: #ECECEC;
      font-size: 13.5px;
    }
    .settings-action-btn {
      width: 100%;
      padding: 11px 14px;
      border-radius: 12px;
      border: none;
      background: #282828;
      color: #ECECEC;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
    }
    .settings-action-btn:hover { background: #323232; }
  </style>
</head>
<body>
  <div class="app-layout">
    <div class="drawer-backdrop" id="drawerBackdrop"></div>
    <aside class="sidebar-drawer" id="sidebarDrawer">
      <div class="drawer-header">
        <div class="drawer-logo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L14.8 9.2L22 12L14.8 14.8L12 22L9.2 14.8L2 12L9.2 9.2L12 2Z"></path>
          </svg>
        </div>
        <div class="drawer-title-group">
          <div class="drawer-title">Auralis AI</div>
          <div class="drawer-subtitle">Conversational Intelligence</div>
        </div>
      </div>

      <button class="new-chat-pill" id="drawerNewChatBtn" type="button">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span>New Chat</span>
      </button>

      <div class="drawer-section-label">Recent Chats</div>
      <div class="history-list" id="historyList"></div>

      <div class="drawer-footer" id="drawerProfileBtn">
        <div class="user-avatar">RJ</div>
        <div class="user-meta">
          <div class="user-name">Auralis Workspace</div>
          <div class="user-email">reeta.jaiswal9999@gmail.com</div>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9B9B9B" stroke-width="2">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
      </div>
    </aside>

    <div class="app-shell">
      <header class="top-bar">
        <button class="icon-btn" id="menuBtn" type="button" aria-label="Open menu">
          <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>

        <div class="mode-pill">
          <button class="mode-btn active" id="modeFastBtn" type="button">Fast</button>
          <button class="mode-btn" id="modeDeepBtn" type="button">Deep Think</button>
        </div>

        <button class="icon-btn" id="topNewChatBtn" type="button" aria-label="New Chat" title="New Chat">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
          </svg>
        </button>
      </header>

      <main class="chat-container" id="chatContainer"></main>

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
            placeholder="Message Auralis..."
          ></textarea>
          <button class="circle-btn" id="sendOrMicBtn" type="button" aria-label="Send">
            <span id="actionIconWrap"></span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="settings-backdrop" id="settingsModal">
    <div class="settings-card">
      <div class="settings-header">
        <span>Auralis AI Settings</span>
        <button class="icon-btn" id="closeSettingsBtn" type="button" style="width:30px;height:30px;">✕</button>
      </div>
      <div class="settings-row">
        <strong>Fast Mode Routing</strong>
        <span>gemini-2.5-flash → gemini-1.5-flash</span>
      </div>
      <div class="settings-row">
        <strong>Deep Think Engine</strong>
        <span>llama-3.3-70b-versatile (Fallback: gemini-2.5-flash)</span>
      </div>
      <button class="settings-action-btn" id="clearChatBtn" type="button">Start New Conversation</button>
    </div>
  </div>

  <script>
    const MIC_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path><path d="M19 10v2a7 7 0 0 1-14 0v-2"></path><line x1="12" y1="19" x2="12" y2="23"></line><line x1="8" y1="23" x2="16" y2="23"></line></svg>';
    const ARROW_UP_SVG = '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="19" x2="12" y2="5"></line><polyline points="5 12 12 5 19 12"></polyline></svg>';
    const EMPTY_STATE_HTML = '<div class="empty-state" id="emptyState"><div class="empty-spark-badge"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L14.8 9.2L22 12L14.8 14.8L12 22L9.2 14.8L2 12L9.2 9.2L12 2Z"></path></svg></div><div class="empty-greeting">What can I help with today?</div></div>';

    let engineMode = localStorage.getItem('auralis_mode') || 'FAST_MODE';
    let attachedImageBase64 = null;
    let isSynthesizing = false;
    let sessions = [];
    try {
      sessions = JSON.parse(localStorage.getItem('auralis_sessions_v2') || '[]');
    } catch {}
    let currentSessionId = sessions[0]?.id || ('sess_' + Date.now());
    let history = sessions[0]?.messages ? [...sessions[0].messages] : [];

    const chatContainer = document.getElementById('chatContainer');
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
    const menuBtn = document.getElementById('menuBtn');
    const sidebarDrawer = document.getElementById('sidebarDrawer');
    const drawerBackdrop = document.getElementById('drawerBackdrop');
    const drawerNewChatBtn = document.getElementById('drawerNewChatBtn');
    const topNewChatBtn = document.getElementById('topNewChatBtn');
    const historyList = document.getElementById('historyList');
    const drawerProfileBtn = document.getElementById('drawerProfileBtn');
    const settingsModal = document.getElementById('settingsModal');
    const closeSettingsBtn = document.getElementById('closeSettingsBtn');
    const clearChatBtn = document.getElementById('clearChatBtn');

    function openDrawer() {
      sidebarDrawer.classList.add('open');
      drawerBackdrop.classList.add('open');
    }
    function closeDrawer() {
      sidebarDrawer.classList.remove('open');
      drawerBackdrop.classList.remove('open');
    }
    menuBtn.onclick = openDrawer;
    drawerBackdrop.onclick = closeDrawer;

    function syncModeUI() {
      modeFastBtn.classList.toggle('active', engineMode === 'FAST_MODE');
      modeDeepBtn.classList.toggle('active', engineMode === 'DEEP_THINK');
    }
    syncModeUI();

    modeFastBtn.onclick = () => { engineMode = 'FAST_MODE'; localStorage.setItem('auralis_mode', engineMode); syncModeUI(); };
    modeDeepBtn.onclick = () => { engineMode = 'DEEP_THINK'; localStorage.setItem('auralis_mode', engineMode); syncModeUI(); };

    function escapeHtml(str) {
      return String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function formatMarkdown(text) {
      const parts = String(text || '').split(/(\`\`\`[\\s\\S]*?\`\`\`)/g);
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

    function saveSessions() {
      if (history.length > 0) {
        const firstUser = history.find(m => m.role === 'user');
        const title = (firstUser?.text || 'New Chat').slice(0, 38);
        const idx = sessions.findIndex(s => s.id === currentSessionId);
        const updated = { id: currentSessionId, title, updatedAt: Date.now(), messages: [...history] };
        if (idx !== -1) sessions[idx] = updated;
        else sessions.unshift(updated);
      }
      localStorage.setItem('auralis_sessions_v2', JSON.stringify(sessions.slice(0, 25)));
      renderDrawerHistory();
    }

    function renderConversation() {
      if (!history || history.length === 0) {
        chatContainer.innerHTML = EMPTY_STATE_HTML;
        return;
      }
      chatContainer.innerHTML = '';
      for (const m of history) {
        const row = document.createElement('div');
        row.className = 'msg-row ' + (m.role === 'user' ? 'user' : 'ai');
        if (m.role === 'user') {
          const bubble = document.createElement('div');
          bubble.className = 'user-bubble';
          bubble.textContent = m.text;
          row.appendChild(bubble);
        } else {
          const aiText = document.createElement('div');
          aiText.className = 'ai-text';
          aiText.innerHTML = formatMarkdown(m.text);
          row.appendChild(aiText);
        }
        chatContainer.appendChild(row);
      }
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function renderDrawerHistory() {
      if (!sessions.length) {
        historyList.innerHTML = '<div class="history-empty">No saved conversations yet</div>';
        return;
      }
      historyList.innerHTML = '';
      sessions.forEach(sess => {
        const item = document.createElement('div');
        item.className = 'history-item' + (sess.id === currentSessionId ? ' active' : '');
        const titleSpan = document.createElement('span');
        titleSpan.className = 'history-item-title';
        titleSpan.textContent = sess.title;
        item.appendChild(titleSpan);

        const delBtn = document.createElement('button');
        delBtn.className = 'history-delete-btn';
        delBtn.type = 'button';
        delBtn.textContent = '✕';
        delBtn.onclick = (e) => {
          e.stopPropagation();
          sessions = sessions.filter(s => s.id !== sess.id);
          localStorage.setItem('auralis_sessions_v2', JSON.stringify(sessions));
          if (currentSessionId === sess.id) {
            if (sessions[0]) {
              currentSessionId = sessions[0].id;
              history = [...sessions[0].messages];
            } else {
              currentSessionId = 'sess_' + Date.now();
              history = [];
            }
            renderConversation();
          }
          renderDrawerHistory();
        };
        item.appendChild(delBtn);

        item.onclick = () => {
          if (isSynthesizing) return;
          currentSessionId = sess.id;
          history = [...(sess.messages || [])];
          renderConversation();
          renderDrawerHistory();
          closeDrawer();
        };
        historyList.appendChild(item);
      });
    }

    function startNewChat() {
      if (isSynthesizing) return;
      saveSessions();
      currentSessionId = 'sess_' + Date.now();
      history = [];
      renderConversation();
      renderDrawerHistory();
      closeDrawer();
    }

    drawerNewChatBtn.onclick = startNewChat;
    topNewChatBtn.onclick = startNewChat;
    drawerProfileBtn.onclick = () => { closeDrawer(); settingsModal.classList.add('open'); };
    closeSettingsBtn.onclick = () => settingsModal.classList.remove('open');
    settingsModal.onclick = (e) => { if (e.target === settingsModal) settingsModal.classList.remove('open'); };
    clearChatBtn.onclick = () => { startNewChat(); settingsModal.classList.remove('open'); };

    renderConversation();
    renderDrawerHistory();

    function syncActionBtn() {
      const hasInput = promptInput.value.trim().length > 0 || Boolean(attachedImageBase64);
      sendOrMicBtn.classList.toggle('send-active', hasInput);
      actionIconWrap.innerHTML = hasInput ? ARROW_UP_SVG : MIC_SVG;
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

    function downscaleAndEncodeImage(file, callback) {
      const reader = new FileReader();
      reader.onload = () => {
        const img = new Image();
        img.onload = () => {
          const maxDim = 1024;
          let width = img.width;
          let height = img.height;
          if (width > maxDim || height > maxDim) {
            const ratio = maxDim / Math.max(width, height);
            width = Math.max(1, Math.round(width * ratio));
            height = Math.max(1, Math.round(height * ratio));
          }
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, width, height);
          const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
          canvas.width = 0;
          canvas.height = 0;
          callback(dataUrl);
        };
        img.onerror = () => callback(String(reader.result));
        img.src = String(reader.result);
      };
      reader.readAsDataURL(file);
    }

    attachBtn.onclick = () => fileInput.click();
    fileInput.onchange = (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;
      downscaleAndEncodeImage(file, (compressedDataUrl) => {
        attachedImageBase64 = compressedDataUrl;
        attachThumbImg.src = attachedImageBase64;
        attachPreviewBar.classList.add('visible');
        syncActionBtn();
      });
    };

    removeAttachBtn.onclick = () => {
      attachedImageBase64 = null;
      fileInput.value = '';
      attachPreviewBar.classList.remove('visible');
      syncActionBtn();
    };

    function extractDeltaFromSseJson(jsonStr) {
      try {
        const obj = JSON.parse(jsonStr);
        if (typeof obj.text === 'string' && !obj.candidates) return obj.text;
        if (typeof obj.reply === 'string') return obj.reply;
        if (typeof obj.error === 'string') return obj.error;
        const parts = obj && obj.candidates && obj.candidates[0] && obj.candidates[0].content && obj.candidates[0].content.parts;
        if (Array.isArray(parts)) {
          let out = '';
          for (let i = 0; i < parts.length; i++) {
            if (parts[i] && !parts[i].thought && typeof parts[i].text === 'string') {
              out += parts[i].text;
            }
          }
          return out;
        }
      } catch (e) {}
      return '';
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
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
          },
          body: JSON.stringify({
            prompt: text,
            imageBase64: image,
            engineMode,
            history: history.slice(-10)
          })
        });

        const aiTextEl = document.createElement('div');
        aiTextEl.className = 'ai-text';
        let accumulated = '';
        let buffer = '';

        const reader = res.body && res.body.getReader ? res.body.getReader() : null;
        if (reader) {
          const decoder = new TextDecoder();
          while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\\n');
            buffer = lines.pop() || '';
            for (let i = 0; i < lines.length; i++) {
              const line = lines[i].trim();
              if (!line || line.startsWith(':')) continue;
              if (line.startsWith('data:')) {
                const jsonPart = line.slice(5).trim();
                if (jsonPart && jsonPart !== '[DONE]') {
                  const delta = extractDeltaFromSseJson(jsonPart);
                  if (delta) {
                    accumulated += delta;
                    if (capsule.parentNode) capsule.remove();
                    if (!aiTextEl.parentNode) aiRow.appendChild(aiTextEl);
                    aiTextEl.innerHTML = formatMarkdown(accumulated);
                    chatContainer.scrollTop = chatContainer.scrollHeight;
                  }
                }
              }
            }
          }
          if (buffer.trim().startsWith('data:')) {
            const jsonPart = buffer.trim().slice(5).trim();
            if (jsonPart && jsonPart !== '[DONE]') {
              const delta = extractDeltaFromSseJson(jsonPart);
              if (delta) accumulated += delta;
            }
          }
        } else {
          const fallbackText = await res.text();
          accumulated = extractDeltaFromSseJson(fallbackText) || fallbackText;
        }

        const cleanReply = accumulated.trim();
        if (cleanReply) {
          if (capsule.parentNode) capsule.remove();
          if (!aiTextEl.parentNode) aiRow.appendChild(aiTextEl);
          aiTextEl.innerHTML = formatMarkdown(cleanReply);
          history.push({ role: 'user', text: text || 'Attached image' });
          history.push({ role: 'model', text: cleanReply });
          saveSessions();
        } else {
          aiRow.innerHTML = '<div class="ai-text"><p>Unable to generate a response right now. Please try again.</p></div>';
        }
      } catch (err) {
        aiRow.innerHTML = '<div class="ai-text"><p>Unable to reach AI service. Please try again.</p></div>';
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

function buildGeminiPayload(rawHistory, prompt, imageBase64, systemInstruction, lowThinkingForLite = false) {
  const contents = [];
  let lastRole = null;
  for (const h of rawHistory) {
    const role = h.role === 'user' ? 'user' : 'model';
    const cleanText = sanitizeCjk(h.text || '').trim();
    if (!cleanText) continue;
    if (contents.length === 0 && role !== 'user') continue;
    if (role === lastRole && contents.length > 0) {
      contents[contents.length - 1].parts.push({ text: '\n' + cleanText });
    } else {
      contents.push({ role, parts: [{ text: cleanText }] });
      lastRole = role;
    }
  }

  const userParts = [];
  if (imageBase64 && imageBase64.includes('base64,')) {
    const [meta, b64] = imageBase64.split('base64,');
    const mimeMatch = meta.match(/data:([^;]+);/);
    const mimeType = (mimeMatch && mimeMatch[1] === 'image/png') ? 'image/png' : 'image/jpeg';
    userParts.push({
      inlineData: {
        mimeType,
        data: b64
      }
    });
  }
  userParts.push({ text: prompt || 'Describe and analyze this image in detail.' });

  if (lastRole === 'user' && contents.length > 0) {
    contents[contents.length - 1].parts.push(...userParts);
  } else {
    contents.push({ role: 'user', parts: userParts });
  }

  const payloadObj = {
    systemInstruction: { parts: [{ text: systemInstruction }] },
    contents
  };
  if (lowThinkingForLite) {
    payloadObj.generationConfig = { thinking_config: { thinking_level: 'low' } };
  }
  return JSON.stringify(payloadObj);
}

const server = http.createServer(async (req, res) => {
  req.setTimeout(0);
  res.setTimeout(0);

  const url = new URL(req.url || '/', `http://localhost:${PORT}`);

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Accept, x-goog-api-key'
    });
    res.end();
    return;
  }

  if (req.method === 'GET' && url.pathname === '/api/models-config') {
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache'
    });
    res.end(JSON.stringify(loadModelsConfig()));
    return;
  }

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
    req.setTimeout(0);
    res.setTimeout(0);

    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no',
      'Access-Control-Allow-Origin': '*'
    });
    res.write(':\n\n');

    const keepAliveTimer = setInterval(() => {
      if (!res.writableEnded) res.write(':\n\n');
    }, 5000);

    req.on('close', () => clearInterval(keepAliveTimer));

    const finishStream = () => {
      clearInterval(keepAliveTimer);
      if (!res.writableEnded) {
        res.end();
      }
    };

    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', async () => {
      try {
        const bodyStr = Buffer.concat(chunks).toString('utf8');
        chunks.length = 0;
        const payload = JSON.parse(bodyStr || '{}');
        const prompt = sanitizeCjk(payload.prompt || '');
        const engineMode = payload.engineMode || 'FAST_MODE';
        const rawHistory = Array.isArray(payload.history) ? payload.history : [];
        const geminiKey = resolveServerSecret('GEMINI_API_KEY') || (payload.geminiKey || '').trim();
        const groqKey = resolveServerSecret('GROQ_API_KEY') || (payload.groqKey || '').trim();
        const imageBase64 = payload.imageBase64 || null;
        const modelsConfig = loadModelsConfig();

        const systemInstruction = engineMode === 'DEEP_THINK'
          ? 'You are Auralis AI, an advanced deep reasoning assistant. Break down complex problems step-by-step, analyze edge cases rigorously, and provide clear, well-structured, accurate answers without filler. Never output Chinese characters.'
          : 'You are Auralis AI, a fast, helpful, and natural conversational assistant. Respond clearly, directly, and concisely. Never output Chinese characters.';

        // 1. Deep Think Engine ('llama-3.3-70b-versatile' via Groq API)
        if (engineMode === 'DEEP_THINK' && groqKey && !imageBase64) {
          for (const groqModel of modelsConfig.deep_think_chain) {
            if (isModelTemporarilyUnavailable(groqModel)) continue;
            try {
              const controller = new AbortController();
              const timer = setTimeout(() => controller.abort(), 5000);
              const groqResp = await fetch('https://api.groq.com/openai/v1/chat/completions', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json',
                  'Authorization': `Bearer ${groqKey}`
                },
                body: JSON.stringify({
                  model: groqModel,
                  messages: [
                    { role: 'system', content: systemInstruction },
                    ...rawHistory.map(m => ({
                      role: m.role === 'user' ? 'user' : 'assistant',
                      content: sanitizeCjk(m.text)
                    })),
                    { role: 'user', content: prompt }
                  ],
                  temperature: 0.5,
                  max_tokens: 2048
                }),
                signal: controller.signal
              });
              clearTimeout(timer);

              if (!groqResp.ok) {
                if (groqResp.status === 404 || groqResp.status === 400) {
                  markModelTemporarilyUnavailable(groqModel);
                }
                groqResp.body?.cancel?.();
                continue;
              }

              const groqData = await groqResp.json();
              const text = sanitizeCjk(groqData?.choices?.[0]?.message?.content || '').trim();
              if (text) {
                res.write(`data: ${JSON.stringify({ candidates: [{ content: { parts: [{ text }] } }] })}\n\n`);
                res.write('data: [DONE]\n\n');
                finishStream();
                return;
              }
            } catch {}
          }
        }

        // 2. Gemini SSE Streaming ('gemini-2.5-flash' -> 'gemini-1.5-flash' -> 'gemini-3.1-flash-lite')
        if (geminiKey) {
          const requestBodyStandard = buildGeminiPayload(rawHistory, prompt, imageBase64, systemInstruction, false);
          const requestBodyLowThinking = buildGeminiPayload(rawHistory, prompt, imageBase64, systemInstruction, true);

          const sseModels = [
            modelsConfig.fast_primary || 'gemini-2.5-flash',
            modelsConfig.fast_fallback || 'gemini-1.5-flash',
            modelsConfig.fast_active_lite || 'gemini-3.1-flash-lite',
            'gemini-3.1-flash-lite-preview'
          ];

          for (const modelName of sseModels) {
            if (isModelTemporarilyUnavailable(modelName)) continue;
            let timer = null;
            try {
              const controller = new AbortController();
              timer = setTimeout(() => controller.abort(), 25000);
              const endpointUrl = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:streamGenerateContent?alt=sse`;
              const bodyToUse = modelName.includes('3.1') ? requestBodyLowThinking : requestBodyStandard;
              const upstreamResp = await fetch(endpointUrl, {
                method: 'POST',
                headers: {
                  'x-goog-api-key': geminiKey,
                  'Content-Type': 'application/json',
                  'Accept': 'text/event-stream'
                },
                body: bodyToUse,
                signal: controller.signal
              });
              clearTimeout(timer);
              timer = null;

              if (!upstreamResp.ok || !upstreamResp.body) {
                if (upstreamResp.status === 404 || upstreamResp.status === 400) {
                  markModelTemporarilyUnavailable(modelName, 600000);
                }
                upstreamResp.body?.cancel?.();
                continue;
              }

              const reader = upstreamResp.body.getReader();
              const decoder = new TextDecoder();
              let streamedAny = false;

              while (true) {
                const { value, done } = await reader.read();
                if (done) break;
                const chunkStr = sanitizeCjk(decoder.decode(value, { stream: true }));
                if (chunkStr && !res.writableEnded) {
                  streamedAny = true;
                  res.write(chunkStr);
                }
              }

              if (streamedAny) {
                if (!res.writableEnded) {
                  res.write('\ndata: [DONE]\n\n');
                }
                finishStream();
                return;
              }
            } catch {
              if (timer) clearTimeout(timer);
            }
          }
        }

        if (!res.writableEnded) {
          res.write(`data: ${JSON.stringify({ text: 'Unable to reach AI service. Please try again.' })}\n\n`);
          res.write('data: [DONE]\n\n');
        }
        finishStream();
      } catch {
        if (!res.writableEnded) {
          res.write(`data: ${JSON.stringify({ text: 'Unable to process request.' })}\n\n`);
          res.write('data: [DONE]\n\n');
        }
        finishStream();
      }
    });
    return;
  }

  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-store, no-cache, must-revalidate'
  });
  res.end(HTML_PAGE);
});

server.timeout = 0;
server.keepAliveTimeout = 0;
server.headersTimeout = 0;

server.listen(PORT, '0.0.0.0', () => {
  console.log('Auralis AI SSE server listening on http://localhost:' + PORT);
});
