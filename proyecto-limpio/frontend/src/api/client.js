import axios from "axios";

// El JWT viaja en una cookie HttpOnly que fija el backend; JavaScript no puede leerlo.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.assign("/login");
    }
    return Promise.reject(error);
  },
);

export default client;
