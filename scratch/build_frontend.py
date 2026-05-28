import os
import shutil
import re

# Paths
base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
frontend_dir = os.path.join(base_dir, 'frontend')
static_dir = os.path.join(base_dir, 'static')

print("Starting static frontend build process...")

# 1. Create Directories
dirs = [
    os.path.join(frontend_dir, 'static', 'css'),
    os.path.join(frontend_dir, 'static', 'js'),
    os.path.join(frontend_dir, 'static', 'images')
]

for d in dirs:
    if not os.path.exists(d):
        os.makedirs(d)
        print(f"Created directory: {d}")

# 2. Copy CSS
shutil.copy(
    os.path.join(static_dir, 'css', 'style.css'),
    os.path.join(frontend_dir, 'static', 'css', 'style.css')
)
print("Copied style.css")

# 3. Copy Lightweight Charts
shutil.copy(
    os.path.join(static_dir, 'js', 'lightweight-charts.standalone.production.js'),
    os.path.join(frontend_dir, 'static', 'js', 'lightweight-charts.standalone.production.js')
)
print("Copied lightweight-charts.standalone.production.js")

# 4. Copy Logos
shutil.copy(
    os.path.join(static_dir, 'images', 'logo_lakshmifx.png'),
    os.path.join(frontend_dir, 'static', 'images', 'logo_lakshmifx.png')
)
shutil.copy(
    os.path.join(static_dir, 'images', 'logo_sarthifxm.png'),
    os.path.join(frontend_dir, 'static', 'images', 'logo_sarthifxm.png')
)
print("Copied company logos")

# 5. Process and Copy app.js
app_js_src = os.path.join(static_dir, 'js', 'app.js')
app_js_dest = os.path.join(frontend_dir, 'static', 'js', 'app.js')

with open(app_js_src, 'r', encoding='utf-8') as f:
    content = f.read()

# Add API_BASE_URL to top
header = """// GLOBAL API BASE CONFIGURATION (REPLACE WITH YOUR CLOUDFLARE OR NGROK TUNNEL URL WHEN LIVE!)
const API_BASE_URL = "http://localhost:5000";

"""
content = header + content

# Replace fetch calls
# Find fetch("/api/... and replace with fetch(`${API_BASE_URL}/api/...
content = re.sub(r'fetch\(\"/api/', 'fetch(`${API_BASE_URL}/api/', content)
content = re.sub(r"fetch\(\'/api/", "fetch(`${API_BASE_URL}/api/", content)

# Replace window.open symbol details URL
content = re.sub(
    r'window\.open\(`/symbol/\$\{symbol\}`', 
    'window.open(`./symbol_detail.html?symbol=${symbol}`', 
    content
)

with open(app_js_dest, 'w', encoding='utf-8') as f:
    f.write(content)

print("Processed and saved app.js with static API path overrides!")

# 6. Process and Compile index.html
index_html_src = os.path.join(base_dir, 'templates', 'index.html')
index_html_dest = os.path.join(frontend_dir, 'index.html')

with open(index_html_src, 'r', encoding='utf-8') as f:
    html = f.read()

# Replace url_for dynamic paths with static relative paths
html = html.replace("{{ url_for('static', filename='css/style.css', v='1.2') }}", "./static/css/style.css?v=1.2")
html = html.replace("{{ url_for('static', filename='js/lightweight-charts.standalone.production.js') }}", "./static/js/lightweight-charts.standalone.production.js")
html = html.replace("{{ url_for('static', filename='js/app.js') }}", "./static/js/app.js")

# Remove admin-only block in Header
header_admin_pat = r'\{\%\s*if\s+role\s*==\s*\'admin\'\s*\%\}[\s\S]*?\{\%\s*else\s*\%\}[\s\S]*?(<a\s+href="\/admin"[\s\S]*?<\/a>)[\s\S]*?\{\%\s*endif\s*\%\}'
html = re.sub(header_admin_pat, r'\1', html)

# Remove broadcast advisory form (which starts with {% if role == 'admin' %} and ends with {% endif %})
advisory_form_pat = r'\{\%\s*if\s+role\s*==\s*\'admin\'\s*\%\}[\s\S]*?advisory-submit-btn[\s\S]*?\{\%\s*endif\s*\%\}'
html = re.sub(advisory_form_pat, '', html)

# Remove Bot Settings form
bot_settings_pat = r'\{\%\s*if\s+role\s*==\s*\'admin\'\s*\%\}[\s\S]*?telegram-token[\s\S]*?\{\%\s*endif\s*\%\}'
html = re.sub(bot_settings_pat, '', html)

with open(index_html_dest, 'w', encoding='utf-8') as f:
    f.write(html)
print("Processed and compiled templates/index.html to frontend/index.html")

# 7. Process and Compile symbol_detail.html
symbol_src = os.path.join(base_dir, 'templates', 'symbol_detail.html')
symbol_dest = os.path.join(frontend_dir, 'symbol_detail.html')

with open(symbol_src, 'r', encoding='utf-8') as f:
    s_html = f.read()

s_html = s_html.replace("{{ url_for('static', filename='css/style.css', v='1.2') }}", "./static/css/style.css?v=1.2")
s_html = s_html.replace("{{ url_for('static', filename='js/lightweight-charts.standalone.production.js') }}", "./static/js/lightweight-charts.standalone.production.js")

# Replace symbol name template variables
s_html = s_html.replace("{{ symbol }}", "Asset")

with open(symbol_dest, 'w', encoding='utf-8') as f:
    f.write(s_html)
print("Processed and compiled templates/symbol_detail.html to frontend/symbol_detail.html")

print("Build complete! C:\\mt5\\frontend is fully configured and ready for GitHub/Vercel deployment.")
