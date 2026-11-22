const API_BASE = window.LIMS_API_BASE || "http://localhost:8080/api";
const list = document.getElementById("book-list");
const message = document.getElementById("message");
const count = document.getElementById("result-count");
const form = document.getElementById("search-form");
const input = document.getElementById("search-input");

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
    card.append(tag, title, isbn, year);
    list.append(card);
  });
}
async function loadBooks(search = "") {
  showMessage("Loading catalogue…");
  count.textContent = "…";
  try {
    const url = new URL(API_BASE + "/books");
    if (search.trim()) url.searchParams.set("search", search.trim());
    const response = await fetch(url);
    if (!response.ok) throw new Error("API returned HTTP " + response.status);
    const books = await response.json();
    if (!Array.isArray(books)) throw new Error("Unexpected API response");
    renderBooks(books);
  } catch (error) {
    count.textContent = "Unavailable";
    showMessage("Could not connect to the backend. Start Spring Boot and check the API URL. " + error.message);
  }
}
form.addEventListener("submit", event => {
  event.preventDefault();
  loadBooks(input.value);
});
loadBooks();