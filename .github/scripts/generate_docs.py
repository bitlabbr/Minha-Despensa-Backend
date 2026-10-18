#!/usr/bin/env python3
import os
import json

def build():
    os.makedirs("_site", exist_ok=True)
    doc_path = "Minha-Despensa-Backend.md"
    
    if os.path.exists(doc_path):
        with open(doc_path, "r", encoding="utf-8") as f:
            markdown_content = f.read()
    else:
        markdown_content = "# Minha Despensa Backend\nDocumentação não encontrada."

    escaped_markdown = json.dumps(markdown_content)

    html_template = f"""<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Minha Despensa Backend — Documentação Oficial</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.5.1/github-markdown.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css">
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        :root {{
            --bg-color: #0d1117;
            --container-bg: #161b22;
            --border-color: #30363d;
            --text-color: #c9d1d9;
            --accent-color: #58a6ff;
        }}
        body {{
            background-color: var(--bg-color);
            color: var(--text-color);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            margin: 0;
            padding: 24px;
            display: flex;
            justify-content: center;
        }}
        .wrapper {{
            max-width: 980px;
            width: 100%;
            background: var(--container-bg);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 40px;
            box-shadow: 0 12px 36px rgba(0,0,0,0.4);
        }}
        .header-bar {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 20px;
            margin-bottom: 30px;
        }}
        .badge {{
            display: inline-block;
            padding: 4px 12px;
            font-size: 12px;
            font-weight: 600;
            border-radius: 20px;
            background: rgba(88, 166, 255, 0.15);
            color: var(--accent-color);
            border: 1px solid rgba(88, 166, 255, 0.3);
        }}
        .markdown-body {{
            color: var(--text-color);
            background-color: transparent !important;
        }}
        .markdown-body pre {{
            background-color: #0d1117 !important;
            border: 1px solid var(--border-color);
        }}
        .mermaid {{
            background: #0d1117;
            padding: 16px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            margin: 20px 0;
            display: flex;
            justify-content: center;
        }}
    </style>
</head>
<body>
    <div class="wrapper">
        <div class="header-bar">
            <div>
                <span class="badge">Spring Boot 4.x / Kotlin / Domus</span>
            </div>
            <div>
                <a href="https://github.com/wjsarq/Minha-Despensa-Backend" style="color: var(--accent-color); text-decoration: none; font-size: 14px; font-weight: 500;">GitHub Repository ➔</a>
            </div>
        </div>
        <div id="content" class="markdown-body"></div>
    </div>

    <script>
        const markdown = {escaped_markdown};
        marked.setOptions({{
            highlight: function(code, lang) {{
                if (lang === 'mermaid') {{
                    return `<div class="mermaid">${{code}}</div>`;
                }}
                const language = highlight.getLanguage(lang) ? lang : 'plaintext';
                return highlight.highlight(code, {{ language }}).value;
            }}
        }});
        document.getElementById('content').innerHTML = marked.parse(markdown);
        mermaid.initialize({{ startOnLoad: true, theme: 'dark' }});
    </script>
</body>
</html>
"""

    with open("_site/index.html", "w", encoding="utf-8") as f:
        f.write(html_template)
    print("Documentation static site generated at _site/index.html")

if __name__ == "__main__":
    build()
