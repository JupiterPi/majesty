FROM node:18-alpine
WORKDIR /app
RUN npm i -g @angular/cli@14
COPY . .
RUN npm ci
EXPOSE 80
CMD ["ng", "serve", "--host", "0.0.0.0", "--port", "80"]
