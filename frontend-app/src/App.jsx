import React, { useState, useEffect } from 'react';
import LoginScreen from './components/LoginScreen';
import MainScreen from './components/MainScreen';

function App() {
  // O estado 'isLoggedIn' agora controla qual tela mostrar.
  // Começamos com 'false' (tela de login).
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // Este hook gerencia as classes do <body>
  useEffect(() => {
    const body = document.body;
    if (isLoggedIn) {
      body.classList.remove('body-login');
      body.classList.add('body-main');
    } else {
      body.classList.remove('body-main');
      body.classList.add('body-login');
    }
    // A função de limpeza remove as classes quando o componente é desmontado
    return () => {
      body.classList.remove('body-login', 'body-main');
    };
  }, [isLoggedIn]); // O hook roda sempre que o estado 'isLoggedIn' muda

  // Para simular o login, podemos passar a função setIsLoggedIn para o LoginScreen
  // Por enquanto, vamos manter a lógica de troca aqui para teste.
  // Para ver a tela principal, mude o valor inicial de useState para 'true'.

  if (isLoggedIn) {
    // Passamos a função para que a tela principal possa fazer logout
    return <MainScreen onLogout={() => setIsLoggedIn(false)} />;
  } else {
    // Passamos a função para que a tela de login possa fazer o login
    return <LoginScreen onLoginSuccess={() => setIsLoggedIn(true)} />;
  }
}

export default App;
