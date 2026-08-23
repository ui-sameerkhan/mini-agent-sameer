/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        buy: "#16c784",
        sell: "#ea3943",
        hold: "#f7a600",
      },
    },
  },
  plugins: [],
};
