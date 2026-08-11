FROM node:20-alpine

WORKDIR /app

# Instala as dependências primeiro para melhor aproveitamento de camadas no cache do Docker
COPY package*.json ./
RUN npm install

# Copia o restante do código da aplicação
COPY . .

# Expor a porta padrão do Vite (3000)
EXPOSE 3000

# Inicia o Vite escutando em todas as interfaces de rede (0.0.0.0) para que possa ser acessado pelo host
CMD ["npm", "run", "dev", "--", "--host"]
