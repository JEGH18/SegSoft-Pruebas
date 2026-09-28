import axios from "axios";

const apiKey = "pk4f9A8sd7F6g5H4j3K2L1";

const client = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: { "X-Api-Key": apiKey },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default client;
