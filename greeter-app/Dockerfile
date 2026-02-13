FROM node:22 AS builder
WORKDIR /app

COPY . .
RUN npm install && npm run build

FROM nginx:alpine

# Install netcat for health checks
RUN apk add --no-cache netcat-openbsd

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
