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
    loadOptions("/authors", "authorIds", "No authors"),
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
  const authorSelect = bookForm.elements["authorIds"];
  const authorIds = Array.from(authorSelect.selectedOptions)
    .map(option => option.value)
    .filter(value => value !== "")
    .map(Number);
  const payload = {
    isbn: data.get("isbn").trim(),
    title: data.get("title").trim(),
    edition: data.get("edition").trim() || null,
    publicationYear: data.get("publicationYear") ? Number(data.get("publicationYear")) : null,
    categoryId: Number(data.get("categoryId")),
    publisherId: data.get("publisherId") ? Number(data.get("publisherId")) : null,
    authorIds: authorIds,
    coverImageUrl: data.get("coverImageUrl").trim() || null
  };
  try {
    await api("/books", { method: "POST", body: JSON.stringify(payload) });
    bookForm.reset();
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
