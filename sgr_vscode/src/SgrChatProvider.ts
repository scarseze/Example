import * as vscode from 'vscode';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

export class SgrChatProvider implements vscode.WebviewViewProvider {

    public static readonly viewType = 'sgr.chatView';
    private _view?: vscode.WebviewView;

    constructor(
        private readonly _extensionUri: vscode.Uri,
    ) { }

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken,
    ) {
        this._view = webviewView;

        webviewView.webview.options = {
            enableScripts: true,
            localResourceRoots: [
                this._extensionUri
            ]
        };

        webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

        webviewView.webview.onDidReceiveMessage(async (data) => {
            switch (data.type) {
                case 'sendMessage': {
                    await this._handleUserMessage(data.value);
                    break;
                }
            }
        });
    }

    private async _handleUserMessage(userQuery: string) {
        if (!this._view) { return; }

        // 1. Show user message in UI (handled by frontend, but we acknowledge)
        // 2. Capture Context (Dynamic)
        const editor = vscode.window.activeTextEditor;
        let contextData = {};

        if (editor) {
            const document = editor.document;
            const selection = editor.selection;
            const text = document.getText(selection);

            const diagnostics = vscode.languages.getDiagnostics(document.uri)
                .map(d => `${d.range.start.line + 1}: [${vscode.DiagnosticSeverity[d.severity]}] ${d.message}`)
                .join('\n');

            const openFiles = vscode.workspace.textDocuments
                .filter(doc => !doc.isUntitled && doc.uri.scheme === 'file')
                .map(doc => doc.uri.fsPath);

            contextData = {
                file_path: document.uri.fsPath,
                content: text ? "" : document.getText(), // Careful with full content size
                selection: text ? {
                    start_line: selection.start.line + 1,
                    end_line: selection.end.line + 1,
                    text: text
                } : null,
                cursor_line: selection.active.line + 1,
                diagnostics: diagnostics ? diagnostics : null,
                open_files: openFiles
            };
        }

        // 3. Send to Server
        const config = vscode.workspace.getConfiguration('sgr');
        const serverUrl = config.get<string>('serverUrl') || "http://localhost:8000/v1/agent/process";

        try {
            const payload = {
                query: userQuery,
                source_app: "vscode_extension_sidebar",
                context: contextData
            };

            const response = await fetch(serverUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(`Server error: ${response.status} ${response.statusText}`);
            }

            const data = await response.json() as any;
            const result = data.result;

            // Render Markdown to HTML
            const md = new MarkdownIt({
                html: true,
                linkify: true,
                typographer: true,
                highlight: function (str, lang) {
                    if (lang && hljs.getLanguage(lang)) {
                        try {
                            return hljs.highlight(str, { language: lang }).value;
                        } catch (__) { }
                    }
                    return '';
                }
            });
            const renderedHtml = md.render(result);

            // 4. Send Response back to Webview
            if (this._view) {
                this._view.webview.postMessage({ type: 'receiveMessage', value: renderedHtml });
            }

        } catch (error: any) {
            if (this._view) {
                this._view.webview.postMessage({ type: 'receiveMessage', value: `❌ Error: ${error.message}` });
            }
        }
    }

    private _getHtmlForWebview(webview: vscode.Webview) {
        const md = new MarkdownIt({
            html: true,
            linkify: true,
            typographer: true,
            highlight: function (str, lang) {
                if (lang && hljs.getLanguage(lang)) {
                    try {
                        return hljs.highlight(str, { language: lang }).value;
                    } catch (__) { }
                }
                return '';
            }
        });

        // We embed the Markdown render logic in the frontend or render on backend?
        // Better to render on backend (here) if we want to use the existing markdown-it setup,
        // BUT for a chat interface, the frontend needs to append messages dynamically.
        // So we will send raw markdown to frontend, and frontend needs a way to render it,
        // OR we render it here and send HTML.
        // Sending HTML is safer/easier given we have markdown-it here.

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        :root {
            --bg-color: var(--vscode-editor-background);
            --text-color: var(--vscode-editor-foreground);
            --input-bg: var(--vscode-input-background);
            --input-fg: var(--vscode-input-foreground);
            --border-color: var(--vscode-sideBar-border);
            --button-bg: var(--vscode-button-background);
            --button-fg: var(--vscode-button-foreground);
        }
        body { font-family: var(--vscode-font-family); padding: 0; margin: 0; display: flex; flex-direction: column; height: 100vh; background: var(--bg-color); color: var(--text-color); }
        
        #chat-container { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 10px; }
        
        .message { padding: 8px 12px; border-radius: 6px; max-width: 90%; word-wrap: break-word; }
        .message.user { align-self: flex-end; background: var(--button-bg); color: var(--button-fg); }
        .message.agent { align-self: flex-start; background: var(--input-bg); border: 1px solid var(--border-color); }
        
        /* Markdown Styles */
        .message.agent pre { background: #1e1e1e; padding: 8px; border-radius: 4px; overflow-x: auto; }
        .message.agent code { font-family: Consolas, monospace; font-size: 85%; }
        .message.agent p { margin: 0 0 8px 0; }
        .message.agent p:last-child { margin: 0; }

        #input-container { padding: 10px; border-top: 1px solid var(--border-color); display: flex; gap: 8px; }
        #message-input { flex: 1; padding: 8px; background: var(--input-bg); color: var(--input-fg); border: 1px solid var(--border-color); border-radius: 4px; outline: none; font-family: inherit; }
        #send-button { padding: 8px 12px; background: var(--button-bg); color: var(--button-fg); border: none; border-radius: 4px; cursor: pointer; }
        #send-button:hover { opacity: 0.9; }
        
        .typing-indicator { font-style: italic; color: #888; font-size: 0.8em; margin-bottom: 5px; display: none; }
        .typing-indicator.visible { display: block; }
        
        /* Highlight.js Theme (minimal) */
        .hljs{display:block;overflow-x:auto;padding:.5em;color:#abb2bf;background:#282c34}.hljs-keyword{color:#c678dd}.hljs-string{color:#98c379}.hljs-title{color:#61aeee}.hljs-comment{color:#5c6370;font-style:italic}

    </style>
</head>
<body>
    <div id="chat-container"></div>
    <div class="typing-indicator" id="typing-indicator">Agent is thinking...</div>
    <div id="input-container">
        <input type="text" id="message-input" placeholder="Ask SGR..." />
        <button id="send-button">Send</button>
    </div>

    <!-- We need a local script to handle UI interaction -->
    <script>
        const vscode = acquireVsCodeApi();
        const chatContainer = document.getElementById('chat-container');
        const messageInput = document.getElementById('message-input');
        const sendButton = document.getElementById('send-button');
        const typingIndicator = document.getElementById('typing-indicator');

        // Setup Highlight.js and Markdown-it?? 
        // No, we receive HTML from extension for the agent response to keep frontend light/secure.
        // Wait, for User message we just show text. For Agent we get HTML.

        sendButton.addEventListener('click', sendMessage);
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        function sendMessage() {
            const text = messageInput.value.trim();
            if (!text) return;

            // Add user message to UI
            addMessage(text, 'user');
            messageInput.value = '';
            
            // Show typing
            typingIndicator.classList.add('visible');

            // Send to Extension
            vscode.postMessage({ type: 'sendMessage', value: text });
        }

        function addMessage(content, type) {
            const div = document.createElement('div');
            div.className = 'message ' + type;
            // For user: textContent (safe). For agent: innerHTML (trusted from extension)
            if (type === 'user') {
                div.textContent = content;
            } else {
                div.innerHTML = content;
            }
            chatContainer.appendChild(div);
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }

        window.addEventListener('message', event => {
            const message = event.data;
            switch (message.type) {
                case 'receiveMessage':
                    typingIndicator.classList.remove('visible');
                    // We need to render the markdown on the extension side before sending here?
                    // Yes, the extension sends 'value' which should be HTML string if it was markdown.
                    // But in _handleUserMessage right now I am sending raw result.
                    // I need to update _handleUserMessage to render MD before sending.
                    
                    // Actually, let's assume the extension SENDS HTML.
                    addMessage(message.value, 'agent');
                    break;
            }
        });
    </script>
</body>
</html>`;
    }
}
