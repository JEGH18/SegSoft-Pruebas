import React from "react";

export default function ProductReview({ review }) {
  return (
    <article className="review">
      <h4>{review.author}</h4>
      <p>{review.comment}</p>
    </article>
  );
}
