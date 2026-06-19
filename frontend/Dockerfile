ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/library/node:20-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci --registry=https://repo.huaweicloud.com/repository/npm/

COPY . .

RUN npm run build

FROM ${DOCKER_REGISTRY}/library/nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
