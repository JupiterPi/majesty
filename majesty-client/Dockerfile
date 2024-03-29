FROM node:18-alpine as build
WORKDIR /app
RUN npm i -g @angular/cli@14
COPY . .
RUN npm ci && ng build

FROM nginx:alpine
COPY --from=build /app/dist/majesty-client /usr/share/nginx/html
EXPOSE 80
