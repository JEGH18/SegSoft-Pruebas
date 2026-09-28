export function showNotification(message) {
  const banner = document.getElementById("notification-banner");
  banner.textContent = message;
  banner.style.display = "block";
}

export function showSearchTerm() {
  const params = new URLSearchParams(window.location.search);
  const results = document.getElementById("search-title");
  results.textContent = params.get("q") ?? "";
}
