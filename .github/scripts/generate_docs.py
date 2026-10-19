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
<html lang="pt-BR" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Minha Despensa Backend — Documentação Oficial</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <link id="hljs-theme" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css">
    <script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/kotlin.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/bash.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/yaml.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
    <style>
        :root[data-theme="dark"] {{
            --bg-primary: #0b0f19;
            --bg-secondary: #111827;
            --bg-surface: #1e293b;
            --bg-code: #0f172a;
            --bg-table-stripe: rgba(30, 41, 59, 0.45);
            --text-primary: #f8fafc;
            --text-secondary: #cbd5e1;
            --text-muted: #94a3b8;
            --border-color: #1e293b;
            --border-subtle: #334155;
            --accent: #38bdf8;
            --accent-hover: #7dd3fc;
            --accent-soft: rgba(56, 189, 248, 0.12);
            --accent-border: rgba(56, 189, 248, 0.3);
            --inline-code-bg: rgba(56, 189, 248, 0.12);
            --inline-code-text: #7dd3fc;
            --blockquote-bg: rgba(56, 189, 248, 0.06);
            --shadow: 0 20px 50px rgba(0, 0, 0, 0.45);
        }}

        :root[data-theme="light"] {{
            --bg-primary: #f8fafc;
            --bg-secondary: #ffffff;
            --bg-surface: #f1f5f9;
            --bg-code: #0f172a;
            --bg-table-stripe: #f8fafc;
            --text-primary: #0f172a;
            --text-secondary: #334155;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --border-subtle: #cbd5e1;
            --accent: #0284c7;
            --accent-hover: #0369a1;
            --accent-soft: rgba(2, 132, 199, 0.1);
            --accent-border: rgba(2, 132, 199, 0.25);
            --inline-code-bg: #f1f5f9;
            --inline-code-text: #0f172a;
            --blockquote-bg: #f8fafc;
            --shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
        }}

        * {{
            box-sizing: border-box;
        }}

        html {{
            scroll-behavior: smooth;
            scroll-padding-top: 84px;
        }}

        body {{
            margin: 0;
            background-color: var(--bg-primary);
            color: var(--text-primary);
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            line-height: 1.7;
            font-size: 16px;
            -webkit-font-smoothing: antialiased;
        }}

        /* Top Navigation Bar */
        .topbar {{
            position: sticky;
            top: 0;
            z-index: 100;
            height: 64px;
            background: rgba(11, 15, 25, 0.82);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 32px;
        }}

        :root[data-theme="light"] .topbar {{
            background: rgba(255, 255, 255, 0.88);
        }}

        .brand {{
            display: flex;
            align-items: center;
            gap: 12px;
            font-weight: 700;
            font-size: 16px;
            color: var(--text-primary);
            text-decoration: none;
        }}

        .brand-icon {{
            width: 32px;
            height: 32px;
            border-radius: 8px;
            background: linear-gradient(135deg, #0284c7, #38bdf8);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-weight: 800;
            font-size: 15px;
        }}

        .badge {{
            display: inline-flex;
            align-items: center;
            padding: 3px 10px;
            font-size: 12px;
            font-weight: 600;
            border-radius: 999px;
            background: var(--accent-soft);
            color: var(--accent);
            border: 1px solid var(--accent-border);
        }}

        .topbar-actions {{
            display: flex;
            align-items: center;
            gap: 12px;
        }}

        .btn {{
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-primary);
            background: var(--bg-surface);
            border: 1px solid var(--border-subtle);
            cursor: pointer;
            text-decoration: none;
            transition: all 0.15s ease;
        }}

        .btn:hover {{
            border-color: var(--accent);
            color: var(--accent);
        }}

        /* Main Layout */
        .layout {{
            max-width: 1380px;
            margin: 0 auto;
            display: grid;
            grid-template-columns: 280px minmax(0, 1fr);
            gap: 40px;
            padding: 36px 32px 80px;
        }}

        /* Sidebar TOC */
        .sidebar {{
            position: sticky;
            top: 96px;
            height: calc(100vh - 120px);
            overflow-y: auto;
            padding-right: 12px;
        }}

        .sidebar-title {{
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            color: var(--text-muted);
            margin-bottom: 12px;
            padding-left: 12px;
        }}

        .toc-list {{
            list-style: none;
            padding: 0;
            margin: 0;
            border-left: 1px solid var(--border-color);
        }}

        .toc-item a {{
            display: block;
            padding: 6px 12px;
            font-size: 13.5px;
            color: var(--text-muted);
            text-decoration: none;
            border-left: 2px solid transparent;
            margin-left: -1px;
            transition: all 0.15s ease;
            line-height: 1.4;
        }}

        .toc-item.level-3 a {{
            padding-left: 26px;
            font-size: 12.5px;
        }}

        .toc-item a:hover,
        .toc-item a.active {{
            color: var(--accent);
            border-left-color: var(--accent);
            background: var(--accent-soft);
            font-weight: 500;
        }}

        /* Content Card */
        .doc-container {{
            background: var(--bg-secondary);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 48px 56px;
            box-shadow: var(--shadow);
            min-width: 0;
        }}

        /* Typography & Markdown Styles */
        .markdown-body h1 {{
            font-size: 2.25rem;
            font-weight: 800;
            letter-spacing: -0.025em;
            color: var(--text-primary);
            margin: 0 0 20px 0;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border-color);
            line-height: 1.25;
        }}

        .markdown-body h2 {{
            font-size: 1.5rem;
            font-weight: 700;
            letter-spacing: -0.015em;
            color: var(--text-primary);
            margin: 48px 0 16px 0;
            padding-bottom: 10px;
            border-bottom: 1px solid var(--border-color);
        }}

        .markdown-body h3 {{
            font-size: 1.2rem;
            font-weight: 600;
            color: var(--text-primary);
            margin: 32px 0 12px 0;
        }}

        .markdown-body p,
        .markdown-body li {{
            color: var(--text-secondary);
            font-size: 15.5px;
        }}

        .markdown-body ul,
        .markdown-body ol {{
            padding-left: 24px;
            margin: 12px 0 20px;
        }}

        .markdown-body li + li {{
            margin-top: 6px;
        }}

        .markdown-body strong {{
            color: var(--text-primary);
            font-weight: 600;
        }}

        .markdown-body a {{
            color: var(--accent);
            text-decoration: none;
            font-weight: 500;
        }}

        .markdown-body a:hover {{
            color: var(--accent-hover);
            text-decoration: underline;
        }}

        .markdown-body hr {{
            border: 0;
            border-top: 1px solid var(--border-color);
            margin: 36px 0;
        }}

        /* Blockquotes / Callouts */
        .markdown-body blockquote {{
            margin: 20px 0;
            padding: 14px 20px;
            border-left: 4px solid var(--accent);
            background: var(--blockquote-bg);
            border-radius: 0 10px 10px 0;
        }}

        .markdown-body blockquote p {{
            margin: 0;
            color: var(--text-primary);
        }}

        /* Inline Code & Pre Blocks */
        .markdown-body :not(pre) > code {{
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.86em;
            padding: 3px 7px;
            border-radius: 6px;
            background: var(--inline-code-bg);
            color: var(--inline-code-text);
            border: 1px solid var(--accent-border);
        }}

        .markdown-body pre {{
            position: relative;
            background: var(--bg-code) !important;
            border: 1px solid var(--border-subtle);
            border-radius: 12px;
            padding: 18px 20px;
            overflow-x: auto;
            margin: 20px 0;
        }}

        .markdown-body pre code {{
            font-family: 'JetBrains Mono', monospace;
            font-size: 13.5px;
            line-height: 1.6;
            color: #e2e8f0;
            background: transparent !important;
            padding: 0;
            border: none;
        }}

        /* Tables */
        .markdown-body table {{
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            margin: 24px 0;
            border: 1px solid var(--border-subtle);
            border-radius: 12px;
            overflow: hidden;
            font-size: 14.5px;
        }}

        .markdown-body thead th {{
            background: var(--bg-surface);
            color: var(--text-primary);
            font-weight: 600;
            text-align: left;
            padding: 13px 16px;
            border-bottom: 1px solid var(--border-subtle);
        }}

        .markdown-body tbody td {{
            padding: 13px 16px;
            color: var(--text-secondary);
            border-bottom: 1px solid var(--border-color);
            vertical-align: top;
        }}

        .markdown-body tbody tr:last-child td {{
            border-bottom: none;
        }}

        .markdown-body tbody tr:nth-child(even) {{
            background: var(--bg-table-stripe);
        }}

        /* Mermaid Diagrams */
        .mermaid-wrapper {{
            background: var(--bg-code);
            border: 1px solid var(--border-subtle);
            border-radius: 12px;
            padding: 24px;
            margin: 24px 0;
            display: flex;
            justify-content: center;
            overflow-x: auto;
        }}

        @media (max-width: 960px) {{
            .layout {{
                grid-template-columns: 1fr;
                padding: 20px 16px;
            }}
            .sidebar {{
                display: none;
            }}
            .doc-container {{
                padding: 28px 20px;
            }}
            .topbar {{
                padding: 0 16px;
            }}
        }}
    </style>
</head>
<body>
    <header class="topbar">
        <a href="#" class="brand">
            <div class="brand-icon">MD</div>
            <span>Minha Despensa Backend</span>
            <span class="badge">Spring Boot 4 · Kotlin · Domus</span>
        </a>
        <div class="topbar-actions">
            <button class="btn" id="theme-toggle" type="button" title="Alternar tema Claro/Escuro">
                <span id="theme-label">☀️ Tema Claro</span>
            </button>
            <a class="btn" href="https://github.com/bitlabbr/Minha-Despensa-Backend" target="_blank" rel="noopener">
                GitHub ↗
            </a>
        </div>
    </header>

    <div class="layout">
        <aside class="sidebar">
            <div class="sidebar-title">Nesta Página</div>
            <ul class="toc-list" id="toc"></ul>
        </aside>
        <main class="doc-container">
            <article id="content" class="markdown-body"></article>
        </main>
    </div>

    <script>
        const markdown = {escaped_markdown};
        const contentEl = document.getElementById('content');

        // 1. Render Markdown with Marked v12
        contentEl.innerHTML = marked.parse(markdown);

        // 2. Transform Mermaid code blocks & highlight code blocks
        document.querySelectorAll('pre code').forEach((block) => {{
            if (block.classList.contains('language-mermaid')) {{
                const pre = block.parentElement;
                const wrapper = document.createElement('div');
                wrapper.className = 'mermaid-wrapper';
                const mermaidDiv = document.createElement('div');
                mermaidDiv.className = 'mermaid';
                mermaidDiv.textContent = block.textContent;
                wrapper.appendChild(mermaidDiv);
                pre.replaceWith(wrapper);
            }} else {{
                hljs.highlightElement(block);
            }}
        }});

        // 3. Build Sidebar Table of Contents (TOC) from h2 and h3
        const tocEl = document.getElementById('toc');
        const headings = contentEl.querySelectorAll('h2, h3');
        headings.forEach((h, idx) => {{
            if (!h.id) {{
                h.id = 'sec-' + idx + '-' + h.textContent.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
            }}
            const li = document.createElement('li');
            li.className = 'toc-item level-' + h.tagName.substring(1);
            const a = document.createElement('a');
            a.href = '#' + h.id;
            a.textContent = h.textContent;
            li.appendChild(a);
            tocEl.appendChild(li);
        }});

        // 4. Initialize Mermaid
        mermaid.initialize({{
            startOnLoad: true,
            theme: 'dark',
            securityLevel: 'loose',
            fontFamily: 'Inter, sans-serif'
        }});

        // 5. Theme Toggle (Dark / Light)
        const toggleBtn = document.getElementById('theme-toggle');
        const themeLabel = document.getElementById('theme-label');
        toggleBtn.addEventListener('click', () => {{
            const root = document.documentElement;
            const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            root.setAttribute('data-theme', next);
            themeLabel.textContent = next === 'dark' ? '☀️ Tema Claro' : '🌙 Tema Escuro';
        }});
    </script>
</body>
</html>
"""

    with open("_site/index.html", "w", encoding="utf-8") as f:
        f.write(html_template)
    print("Documentation static site generated at _site/index.html")


if __name__ == "__main__":
    build()
