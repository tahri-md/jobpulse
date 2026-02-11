# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files first (cache npm install)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npx ng build --configuration production

# ---- Runtime stage ----
FROM nginx:alpine
COPY --from=build /app/dist/jobpulse-ui/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
