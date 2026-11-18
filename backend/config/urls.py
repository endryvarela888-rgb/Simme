from django.contrib import admin
from django.urls import include, path
from django.conf import settings
from django.conf.urls.static import static
from django.http import HttpResponse
from django.utils.html import escape
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

@api_view(["GET"])
@permission_classes([AllowAny])
def health(_request):
    return Response({"status": "ok", "app": "Simmer"})

def legal_page(title, body):
    contact = escape(settings.SIMMER_CONTACT_EMAIL)
    html = f"""<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>{escape(title)} · Simmer</title><style>body{{font:16px/1.6 system-ui;max-width:760px;margin:48px auto;padding:0 20px;color:#2b2118;background:#fff8f0}}a,button{{color:#b4410b}}input,button{{font:inherit;padding:10px;margin:6px 0}}.card{{background:white;border-radius:16px;padding:24px}}</style>
    <main class="card"><h1>{escape(title)}</h1>{body}<p>Contact: <a href="mailto:{contact}">{contact}</a></p><p><small>Draft for Simmer MVP. Replace the support contact and review for applicable jurisdictions before publication.</small></p></main></html>"""
    return HttpResponse(html)

def privacy(_request):
    return legal_page("Privacy policy", """
      <p>Simmer processes your email address, display name, optional profile photo, account security data, saved recipes, votes, comments, chat messages, coin balance and purchase/subscription verification records to provide the app. Passwords are stored as salted hashes. Recipe and account requests are processed by Simmer's API hosted on a VPS.</p>
      <p>If you publish a recipe, its photo, ingredients, steps and display name are visible to other users. Your optional profile photo appears with your comments and replies. Comments, replies and recipe ratings are public. Users can report recipes, comments, accounts and AI answers, and block accounts; moderation reports and the reported content may be retained for review.</p>
      <p>When you use Chef AI, your question and relevant recipe context are sent to the AI service provider configured by Simmer (currently OpenAI) to generate a reply. Chat history is available in the app for three days. Do not include sensitive personal information. AI messages may be inaccurate; check allergens and food safety.</p>
      <p>When you buy coins or a subscription, Google Play processes the payment. Simmer receives purchase verification information, such as product identifiers and purchase tokens, to validate the transaction and grant its benefits; Simmer does not receive your full payment card details. Email addresses are shared with the email delivery provider to send verification and account recovery messages.</p>
      <p>When advertising is enabled, the mobile advertising SDK may process device and ad interaction data under your consent choices and the provider's terms. You can review available consent settings in the app's Privacy choices control when it is shown.</p>
      <p>You can request account deletion in the app or use the <a href="/account-deletion/">account deletion page</a>. Deletion removes account-associated data, subject to records that must be retained for security, purchase accounting or legal obligations. Chat history is retained for three days; moderation reports may retain a copy of reported content as described above.</p>""")

def terms(_request):
    return legal_page("Terms of use", """
      <p>Simmer provides recipe information for general cooking guidance. Check ingredient labels, allergens, temperatures and food safety requirements for your circumstances. Recipe content is attributed to its source where applicable.</p>
      <p>Keep your account credentials private. Do not abuse, disrupt or attempt to gain unauthorized access to the service. Simmer may change or suspend features to maintain the service.</p>
      <p>Before publishing a recipe or comment, you must accept these rules. Only publish recipes and photos you have the right to share. Do not post unlawful, abusive, harassing, hateful, spam, misleading or unsafe content. Users can report recipes, comments and accounts, and block accounts to hide their recipes and comments. Simmer reviews reports and may remove content or restrict accounts that violate these rules.</p>
      <p>Chef AI is an automated cooking assistant and can make mistakes. Verify food safety, allergens and cooking temperatures independently. Do not use it for medical, emergency or non-cooking advice. You can report an offensive or unsafe AI response from within the chat.</p>
      <p>These draft terms must be reviewed and completed before public release, including operator identity, governing law, liability and dispute contact information.</p>""")

def account_deletion(_request):
    return legal_page("Delete your Simmer account", """
      <p>Enter the email address on your account. We will send a one-time confirmation code. Confirming permanently deletes the account and its saved recipes.</p>
      <form id="request"><label>Email<br><input type="email" name="email" required></label><br><button>Send deletion code</button></form>
      <form id="confirm" hidden><label>Confirmation code<br><input name="code" inputmode="numeric" pattern="[0-9]{6}" required></label><br><button>Delete my account</button></form><p id="message" role="status"></p>
      <script>const msg=document.getElementById('message'),req=document.getElementById('request'),confirmForm=document.getElementById('confirm');let email='';async function post(path,data){{const r=await fetch('/api/v1/auth/account-deletion/'+path,{{method:'POST',headers:{{'Content-Type':'application/json'}},body:JSON.stringify(data)}});const d=await r.json();if(!r.ok)throw Error(d.detail||'Request failed');return d}}req.onsubmit=async e=>{{e.preventDefault();email=new FormData(req).get('email');try{{const d=await post('request/',{{email}});msg.textContent=d.detail;confirmForm.hidden=false}}catch(x){{msg.textContent=x.message}}}};confirmForm.onsubmit=async e=>{{e.preventDefault();try{{const d=await post('confirm/',{{email,code:new FormData(confirmForm).get('code')}});msg.textContent=d.detail;req.hidden=true;confirmForm.hidden=true}}catch(x){{msg.textContent=x.message}}}};</script>""")

urlpatterns = [path("admin/", admin.site.urls), path("api/v1/health/", health),
    path("api/v1/auth/", include("apps.accounts.urls")), path("api/v1/", include("apps.recipes.urls")),
    path("api/v1/coins/", include("apps.coins.urls")), path("api/v1/chef/", include("apps.chef.urls")),
    path("privacy/", privacy), path("terms/", terms), path("account-deletion/", account_deletion)]
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
