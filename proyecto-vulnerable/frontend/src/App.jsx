import React, { useEffect, useState } from "react";
import client from "./api/client";
import ProductReview from "./components/ProductReview";
import { showSearchTerm } from "./utils/notifications";

export default function App() {
  const [reviews, setReviews] = useState([]);

  useEffect(() => {
    showSearchTerm();
    client.get("/reviews").then((res) => setReviews(res.data));
  }, []);

  return (
    <main>
      <div id="notification-banner" />
      <h2 id="search-title" />
      {reviews.map((review) => (
        <ProductReview key={review.id} review={review} />
      ))}
    </main>
  );
}
