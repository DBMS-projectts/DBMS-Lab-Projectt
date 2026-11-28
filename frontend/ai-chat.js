// Floating "Ask Library AI" chat box. Load AFTER app.js (it reuses api() and authToken()).
(function () {
  const style = document.createElement("style");
  style.textContent = `
    .ai-fab{position:fixed;right:20px;bottom:20px;z-index:50;padding:12px 18px;border:0;border-radius:999px;
      background:#1f4e79;color:#fff;font-weight:600;cursor:pointer;box-shadow:0 4px 14px rgba(0,0,0,.25)}
    .ai-panel{position:fixed;right:20px;bottom:76px;z-index:50;width:min(360px,calc(100vw - 40px));height:460px;
      display:flex;flex-direction:column;background:#fff;color:#1b1b1b;border:1px solid #d0d7de;border-radius:12px;
      box-shadow:0 8px 28px rgba(0,0,0,.25);overflow:hidden}
    .ai-panel[hidden]{display:none}
    .ai-head{padding:10px 14px;background:#1f4e79;color:#fff;font-weight:600}
    .ai-log{flex:1;overflow-y:auto;padding:10px;display:flex;flex-direction:column;gap:8px}
    .ai-msg{max-width:85%;padding:8px 10px;border-radius:10px;white-space:pre-wrap;line-height:1.4;font-size:14px}
    .ai-msg.user{align-self:flex-end;background:#dbeafe}
    .ai-msg.bot{align-self:flex-start;background:#f1f3f5}
    .ai-chips{display:flex;flex-wrap:wrap;gap:6px;padding:0 10px 8px}
    .ai-chips button{border:1px solid #1f4e79;background:#fff;color:#1f4e79;border-radius:999px;padding:4px 10px;
      font-size:12px;cursor:pointer}
    .ai-form{display:flex;gap:6px;padding:10px;border-top:1px solid #e5e7eb}
    .ai-form input{flex:1;padding:8px;border:1px solid #c5ccd3;border-radius:8px;font-size:14px}
    .ai-form button{padding:8px 14px;border:0;border-radius:8px;background:#1f4e79;color:#fff;cursor:pointer}
    .ai-form button:disabled{opacity:.6;cursor:wait}`;
  document.head.append(style);

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
  }

  const fab = el("button", "ai-fab", "Ask Library AI");
  fab.type = "button";
  const panel = el("div", "ai-panel");
  panel.hidden = true;
  const log = el("div", "ai-log");
  const chips = el("div", "ai-chips");
  const form = el("form", "ai-form");
  const input = el("input");
  input.placeholder = "Ask about books, loans, fines…";
  input.maxLength = 500;
  const send = el("button", "", "Send");
  send.type = "submit";
  form.append(input, send);
  panel.append(el("div", "ai-head", "Library AI Assistant"), log, chips, form);
  document.body.append(fab, panel);

  function add(role, text) {
    const bubble = el("div", "ai-msg " + role, text);
    log.append(bubble);
    log.scrollTop = log.scrollHeight;
    return bubble;
  }

  ["Database er boi ache?", "Amar loan koyta?", "Amar fine koto?", "Ki poro suggest koro"].forEach(text => {
    const chip = el("button", "", text);
    chip.type = "button";
    chip.addEventListener("click", () => { input.value = text; form.requestSubmit(); });
    chips.append(chip);
  });

  add("bot", "Hi! Boi khuja, loan, fine, recommendation ba library niyom niye jiggesh koro.");

  fab.addEventListener("click", () => { panel.hidden = !panel.hidden; if (!panel.hidden) input.focus(); });

  form.addEventListener("submit", async event => {
    event.preventDefault();
    const question = input.value.trim();
    if (!question) return;
    add("user", question);
    input.value = "";
    if (!authToken()) { add("bot", "Please login first."); return; }
    const pending = add("bot", "Thinking…");
    send.disabled = true;
    try {
      const result = await api("/ai/chat", { method: "POST", body: JSON.stringify({ question }) });
      pending.textContent = result.answer;
    } catch (error) {
      pending.textContent = "Sorry, could not answer: " + error.message;
    } finally {
      send.disabled = false;
      log.scrollTop = log.scrollHeight;
    }
  });
})();
