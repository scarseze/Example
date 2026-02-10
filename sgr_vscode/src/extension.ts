import * as vscode from 'vscode';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import { SgrChatProvider } from './SgrChatProvider';

export function activate(context: vscode.ExtensionContext) {
    console.log('Congratulations, your extension "sgr-agent-client" is now active!');

    // Register Sidebar Chat Provider
    const provider = new SgrChatProvider(context.extensionUri);
    context.subscriptions.push(
        vscode.window.registerWebviewViewProvider(SgrChatProvider.viewType, provider)
    );

    // Register Command (Ask SGR)
    // We can potentially redirect this to the chat, but for now let's keep it legacy-independent
    // or maybe make it open the chat?
    // Let's keep duplicate logic for a moment to ensure legacy works until we verify Sidebar.

    let disposable = vscode.commands.registerCommand('sgr.ask', async () => {
        try {
            const editor = vscode.window.activeTextEditor;
            if (!editor) {
                vscode.window.showErrorMessage('No active editor found.');
                return;
            }

            // 1. Capture Context
            const document = editor.document;
            const selection = editor.selection;
            const text = document.getText(selection);
            const filePath = document.uri.fsPath;
            const cursorLine = selection.active.line + 1;

            // Get Diagnostics
            const diagnostics = vscode.languages.getDiagnostics(document.uri)
                .map(d => `${d.range.start.line + 1}: [${vscode.DiagnosticSeverity[d.severity]}] ${d.message}`)
                .join('\n');

            // Get Open Files
            const openFiles = vscode.workspace.textDocuments
                .filter(doc => !doc.isUntitled && doc.uri.scheme === 'file')
                .map(doc => doc.uri.fsPath);

            const contextData = {
                file_path: filePath,
                content: text ? "" : document.getText(),
                selection: text ? {
                    start_line: selection.start.line + 1,
                    end_line: selection.end.line + 1,
                    text: text
                } : null,
                cursor_line: cursorLine,
                diagnostics: diagnostics ? diagnostics : null,
                open_files: openFiles
            };

            // 2. Ask User
            const userQuery = await vscode.window.showInputBox({
                placeHolder: "What do you want to ask SGR about this code?",
                prompt: "Ask SGR Agent"
            });

            if (!userQuery) return;

            // 3. Prepare UI
            const panel = vscode.window.createWebviewPanel(
                'sgrResponse',
                'SGR Agent',
                vscode.ViewColumn.Beside,
                { enableScripts: true }
            );

            panel.webview.html = getWebviewContent(userQuery, null, true);

            // 4. Send to Server
            const config = vscode.workspace.getConfiguration('sgr');
            const serverUrl = config.get<string>('serverUrl') || "http://localhost:8000/v1/agent/process";

            try {
                const payload = {
                    query: userQuery,
                    source_app: "vscode_extension",
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

                panel.webview.html = getWebviewContent(userQuery, result, false);

            } catch (error: any) {
                panel.webview.html = getWebviewContent(userQuery, `❌ Error: ${error.message}`, false);
                vscode.window.showErrorMessage(`Error communicating with SGR: ${error.message}`);
            }

        } catch (err) {
            console.error("Critical error:", err);
            vscode.window.showErrorMessage("Critical error in SGR extension.");
        }
    });

    context.subscriptions.push(disposable);
}

function getWebviewContent(query: string, response: string | null, isLoading: boolean) {
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
            return ''; // use external default escaping
        }
    });

    let contentHtml = "";
    if (isLoading) {
        contentHtml = `
            <div class="loader-container">
                <div class="spinner"></div>
                <p>Thinking...</p>
            </div>`;
    } else {
        contentHtml = md.render(response || "");
    }

    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SGR Response</title>
    <style>
        :root {
            --bg-color: #1e1e1e;
            --text-color: #d4d4d4;
            --code-bg: #2d2d2d;
            --link-color: #3794ff;
            --border-color: #454545;
        }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            padding: 20px; 
            line-height: 1.6; 
            color: var(--text-color); 
            background-color: var(--bg-color); 
        }
        
        /* Highlight.js Atom One Dark Theme */
        .hljs{display:block;overflow-x:auto;padding:.5em;color:#abb2bf;background:#282c34}.hljs-comment,.hljs-quote{color:#5c6370;font-style:italic}.hljs-doctag,.hljs-keyword,.hljs-formula{color:#c678dd}.hljs-section,.hljs-name,.hljs-selector-tag,.hljs-deletion,.hljs-subst{color:#e06c75}.hljs-literal{color:#56b6c2}.hljs-string,.hljs-regexp,.hljs-addition,.hljs-attribute,.hljs-meta-string{color:#98c379}.hljs-built_in,.hljs-class .hljs-title{color:#e6c07b}.hljs-attr,.hljs-variable,.hljs-template-variable,.hljs-type,.hljs-selector-class,.hljs-selector-attr,.hljs-selector-pseudo,.hljs-number{color:#d19a66}.hljs-symbol,.hljs-bullet,.hljs-link,.hljs-meta,.hljs-selector-id,.hljs-title{color:#61aeee}.hljs-emphasis{font-style:italic}.hljs-strong{font-weight:bold}.hljs-link{text-decoration:underline}

        h1, h2, h3 { color: #fff; margin-top: 24px; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
        p { margin: 16px 0; }
        a { color: var(--link-color); text-decoration: none; }
        
        pre { 
            background-color: #282c34; 
            padding: 16px; 
            overflow: auto; 
            border-radius: 6px; 
            margin-bottom: 16px; 
        }
        code { font-family: Consolas, monospace; }
        
        .user-query { 
            font-style: italic; 
            color: #8b949e; 
            margin-bottom: 20px; 
            border-left: 3px solid var(--link-color); 
            padding-left: 10px; 
            background: rgba(55, 148, 255, 0.1);
            padding: 10px;
        }

        .loader-container { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 200px; color: #8b949e; }
        .spinner {
            border: 4px solid rgba(255, 255, 255, 0.1);
            width: 36px;
            height: 36px;
            border-radius: 50%;
            border-left-color: var(--link-color);
            animation: spin 1s linear infinite;
            margin-bottom: 10px;
        }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    </style>
</head>
<body>
    <div class="user-query">Q: ${query}</div>
    <div id="content">
        ${contentHtml}
    </div>
</body>
</html>`;
}

export function deactivate() { }
