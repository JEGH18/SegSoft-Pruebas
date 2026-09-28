export function showNotification(message) {
  const banner = document.getElementById("notification-banner");
  banner.innerHTML = message;
  banner.style.display = "block";
}

export function showSearchTerm() {
  const params = new URLSearchParams(window.location.search);
  const results = document.getElementById("search-title");
  results.innerHTML = params.get("q");
}
