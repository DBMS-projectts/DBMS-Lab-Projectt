const API_BASE = window.LIMS_API_BASE || "http://localhost:8080/api";
const list = document.getElementById("book-list");
const message = document.getElementById("message");
const count = document.getElementById("result-count");
const form = document.getElementById("search-form");
const input = document.getElementById("search-input");
const bookForm = document.getElementById("book-form");
const formMessage = document.getElementById("form-message");
const loginForm = document.getElementById("login-form");
const authMessage = document.getElementById("auth-message");
const loginBox = document.getElementById("login-box");
const userBox = document.getElementById("user-box");
const currentUser = document.getElementById("current-user");
const currentRole = document.getElementById("current-role");
const logoutButton = document.getElementById("logout-button");
const dashboard = document.getElementById("dashboard");
const statGrid = document.getElementById("stat-grid");
const authorSearch = document.getElementById("author-search");
const authorList = document.getElementById("author-list");
const authorStatus = document.getElementById("author-status");

function authToken() { return sessionStorage.getItem("limsAuth"); }
function authHeader() {
  const token = authToken();
  return token ? { "Authorization": "Basic " + token } : {};
}
function showMessage(text) { message.textContent = text; }
function renderBooks(books) {
  list.replaceChildren();
  count.textContent = books.length + (books.length === 1 ? " book" : " books");
  if (!books.length) { showMessage("No books found. Try another search."); return; }
  showMessage("");
  books.forEach(book => {
    const card = document.createElement("article");
    card.className = "book";
    if (book.coverImageUrl) {
      const cover = document.createElement("img");
      cover.className = "cover";
      cover.src = book.coverImageUrl;
      cover.alt = "";
      cover.addEventListener("error", () => cover.remove());
      card.append(cover);
    }
    const title = document.createElement("h3");
    title.textContent = book.title || "Untitled book";
    const isbn = document.createElement("p");
    isbn.textContent = "ISBN: " + (book.isbn || "Not available");
    const year = document.createElement("p");
    year.textContent = "Publication year: " + (book.publicationYear || "Not specified");
    const tag = document.createElement("span");
    tag.className = "tag";
    tag.textContent = book.categoryName || "Library book";
    card.append(tag, title, isbn, year);
    if (Array.isArray(book.authors) && book.authors.length) {
      const authors = document.createElement("p");
      authors.textContent = "By " + book.authors.map(author => author.name).join(", ");
      card.append(authors);
    }
    const remove = document.createElement("button");
    remove.className = "delete-button";
    remove.type = "button";
    remove.textContent = "Delete";
    remove.addEventListener("click", () => deleteBook(book.id));
    card.append(remove);
    list.append(card);
  });
}
async function api(path, options = {}) {
  const response = await fetch(API_BASE + path, {
    ...options,
    headers: { "Content-Type": "application/json", ...authHeader(), ...(options.headers || {}) }
  });
  if (!response.ok) {
    let detail = "HTTP " + response.status;
    try { const body = await response.json(); detail = body.detail || body.message || detail; } catch (_) {}
    throw new Error(detail);
  }
  if (response.status === 204) return null;
  return response.json();
}
async function loadBooks(search = "") {
  showMessage("Loading catalogue…");
  count.textContent = "…";
  try {
    const params = new URLSearchParams();
    if (search.trim()) params.set("search", search.trim());
    const books = await api("/books" + (params.size ? "?" + params : ""));
    if (!Array.isArray(books)) throw new Error("Unexpected API response");
    renderBooks(books);
  } catch (error) {
    count.textContent = "Unavailable";
    showMessage("Could not load catalogue: " + error.message);
  }
}
async function loadOptions(path, selectName, emptyLabel) {
  const select = bookForm.elements[selectName];
  try {
    const rows = await api(path);
    select.replaceChildren(new Option(emptyLabel, ""));
    rows.forEach(row => select.add(new Option(row.name, row.id)));
  } catch (error) {
    select.replaceChildren(new Option("Could not load options", ""));
    formMessage.textContent = "Dropdown loading failed: " + error.message;
  }
}

/* ---- Authors picker -------------------------------------------------------
   Authors are a multi-select, so they do NOT use loadOptions(): a fake
   "No authors" <option> inside the list looks like a real author and can be
   "selected" by mistake. Instead: a searchable checkbox list, with the
   selection kept in a Set so searching/filtering never loses chosen authors.
   An empty selection is valid (BookRequest.authorIds is optional) and is
   reported by the status line under the list, not by a fake option. */
let allAuthors = [];
const selectedAuthorIds = new Set();

function updateAuthorStatus() {
  const n = selectedAuthorIds.size;
  authorStatus.textContent = n === 0 ? "No author selected"
    : n === 1 ? "1 author selected"
    : n + " authors selected";
}
function renderAuthorList() {
  const term = authorSearch.value.trim().toLowerCase();
  const visible = allAuthors.filter(author => author.name.toLowerCase().includes(term));
  authorList.replaceChildren();
  if (!allAuthors.length) {
    const note = document.createElement("p");
    note.className = "author-empty";
    note.textContent = "No authors yet. Add an author first, then it will appear here.";
    authorList.append(note);
    return;
  }
  if (!visible.length) {
    const note = document.createElement("p");
    note.className = "author-empty";
    note.textContent = "No authors match your search.";
    authorList.append(note);
    return;
  }
  visible.forEach(author => {
    const row = document.createElement("label");
    row.className = "author-option";
    const box = document.createElement("input");
    box.type = "checkbox";
    box.value = author.id;
    box.checked = selectedAuthorIds.has(author.id);
    box.addEventListener("change", () => {
      if (box.checked) selectedAuthorIds.add(author.id);
      else selectedAuthorIds.delete(author.id);
      updateAuthorStatus();
    });
    const name = document.createElement("span");
    name.textContent = author.name;
    row.append(box, name);
    authorList.append(row);
  });
}
async function loadAuthors() {
  try {
    const rows = await api("/authors");
    allAuthors = (Array.isArray(rows) ? rows : [])
      .slice()
      .sort((a, b) => a.name.localeCompare(b.name));
    selectedAuthorIds.clear();
    authorSearch.value = "";
    renderAuthorList();
    updateAuthorStatus();
  } catch (error) {
    authorList.replaceChildren();
    const note = document.createElement("p");
    note.className = "author-empty";
    note.textContent = "Could not load authors.";
    authorList.append(note);
    formMessage.textContent = "Author loading failed: " + error.message;
  }
}
authorSearch.addEventListener("input", renderAuthorList);
/* ---- end authors picker -------------------------------------------------- */

async function deleteBook(id) {
  if (!window.confirm("Delete this book? Books with copies or borrowing history cannot be deleted.")) return;
  try { await api("/books/" + encodeURIComponent(id), { method: "DELETE" }); await loadBooks(input.value); }
  catch (error) { showMessage("Delete failed: " + error.message); }
}
const STAT_LABELS = [
  ["totalBooks", "Books"],
  ["totalCopies", "Copies"],
  ["availableCopies", "Available copies"],
  ["borrowedCopies", "Borrowed copies"],
  ["totalMembers", "Members"],
  ["activeMembers", "Active members"],
  ["activeBorrowings", "Active loans"],
  ["overdueBorrowings", "Overdue loans"],
  ["pendingReservations", "Pending reservations"],
  ["totalFineCollected", "Fines collected"],
  ["totalFineOutstanding", "Fines outstanding"]
];
async function loadDashboard() {
  if (!authToken()) { dashboard.hidden = true; return; }
  try {
    const stats = await api("/dashboard/stats");
    statGrid.replaceChildren();
    STAT_LABELS.forEach(([key, label]) => {
      const card = document.createElement("div");
      card.className = "stat";
      const value = document.createElement("strong");
      value.textContent = stats[key] != null ? stats[key] : "–";
      const caption = document.createElement("span");
      caption.textContent = label;
      card.append(value, caption);
      statGrid.append(card);
    });
    dashboard.hidden = false;
  } catch (error) {
    dashboard.hidden = true;
  }
}
function showLoggedIn(me) {
  loginBox.hidden = true;
  userBox.hidden = false;
  currentUser.textContent = me.fullName + " (" + me.email + ")";
  currentRole.textContent = me.role;
  authMessage.textContent = "";
}
function showLoggedOut() {
  loginBox.hidden = false;
  userBox.hidden = true;
  dashboard.hidden = true;
}
async function refreshAll() {
  await Promise.allSettled([
    loadBooks(input.value),
    loadOptions("/categories", "categoryId", "Choose category"),
    loadOptions("/publishers", "publisherId", "No publisher"),
    loadAuthors(),
    loadDashboard()
  ]);
}
loginForm.addEventListener("submit", async event => {
  event.preventDefault();
  const data = new FormData(loginForm);
  const email = data.get("email").trim();
  const token = btoa(email + ":" + data.get("password"));
  authMessage.textContent = "Signing in…";
  try {
    const response = await fetch(API_BASE + "/auth/me", {
      headers: { "Authorization": "Basic " + token }
    });
    if (!response.ok) throw new Error("Invalid email or password");
    const me = await response.json();
    sessionStorage.setItem("limsAuth", token);
    loginForm.reset();
    showLoggedIn(me);
    await refreshAll();
  } catch (error) {
    sessionStorage.removeItem("limsAuth");
    authMessage.textContent = "Login failed: " + error.message;
  }
});
logoutButton.addEventListener("click", async () => {
  sessionStorage.removeItem("limsAuth");
  showLoggedOut();
  await refreshAll();
});
form.addEventListener("submit", event => {
  event.preventDefault();
  loadBooks(input.value);
});
bookForm.addEventListener("submit", async event => {
  event.preventDefault();
  formMessage.textContent = "Saving book…";
  const data = new FormData(bookForm);
  const payload = {
    isbn: data.get("isbn").trim(),
    title: data.get("title").trim(),
    edition: data.get("edition").trim() || null,
    publicationYear: data.get("publicationYear") ? Number(data.get("publicationYear")) : null,
    categoryId: Number(data.get("categoryId")),
    publisherId: data.get("publisherId") ? Number(data.get("publisherId")) : null,
    authorIds: Array.from(selectedAuthorIds),
    coverImageUrl: data.get("coverImageUrl").trim() || null
  };
  try {
    await api("/books", { method: "POST", body: JSON.stringify(payload) });
    bookForm.reset();
    selectedAuthorIds.clear();
    authorSearch.value = "";
    renderAuthorList();
    updateAuthorStatus();
    formMessage.textContent = "Book added successfully.";
    await loadBooks(input.value);
  } catch (error) { formMessage.textContent = "Could not save book: " + error.message; }
});
(async function init() {
  if (authToken()) {
    try {
      const me = await api("/auth/me");
      showLoggedIn(me);
    } catch (error) {
      sessionStorage.removeItem("limsAuth");
      showLoggedOut();
    }
  }
  await refreshAll();
})();
