const API_BASE = window.LIMS_API_BASE || "http://localhost:8080/api";
const list = document.getElementById("book-list");
const message = document.getElementById("message");
const count = document.getElementById("result-count");
const form = document.getElementById("search-form");
const input = document.getElementById("search-input");
const bookForm = document.getElementById("book-form");
const formMessage = document.getElementById("form-message");

function showMessage(text) { message.textContent = text; }
function renderBooks(books) {
  list.replaceChildren();
  count.textContent = books.length + (books.length === 1 ? " book" : " books");
  if (!books.length) { showMessage("No books found. Try another search."); return; }
  showMessage("");
  books.forEach(book => {
    const card = document.createElement("article");
    card.className = "book";
    const title = document.createElement("h3");
    title.textContent = book.title || "Untitled book";
    const isbn = document.createElement("p");
    isbn.textContent = "ISBN: " + (book.isbn || "Not available");
    const year = document.createElement("p");
    year.textContent = "Publication year: " + (book.publicationYear || "Not specified");
    const tag = document.createElement("span");
    tag.className = "tag";
    tag.textContent = book.categoryName || "Library book";
    const remove = document.createElement("button");
    remove.className = "delete-button";
    remove.type = "button";
    remove.textContent = "Delete";
    remove.addEventListener("click", () => deleteBook(book.id));
    card.append(tag, title, isbn, year, remove);
    list.append(card);
  });
}
async function api(path, options = {}) {
  const response = await fetch(API_BASE + path, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
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
    publisherId: data.get("publisherId") ? Number(data.get("publisherId")) : null
  };
  try {
    await api("/books", { method: "POST", body: JSON.stringify(payload) });
    bookForm.reset();
    formMessage.textContent = "Book added successfully.";
    await loadBooks(input.value);
  } catch (error) { formMessage.textContent = "Could not save book: " + error.message; }
});
loadOptions("/categories", "categoryId", "Choose category");
loadOptions("/publishers", "publisherId", "No publisher");
loadBooks();
