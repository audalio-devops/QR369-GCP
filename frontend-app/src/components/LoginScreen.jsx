import React, { useState } from 'react';

function LoginScreen({ onLoginSuccess }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleLogin = async () => {
        if (!username || !password) {
            setError("Preencha usuário e senha.");
            return;
        }

        setIsLoading(true);
        setError('');

        try {
            const resp = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password })
            });

            if (resp.ok) {
                // Sucesso! Chama a função do App.jsx para mudar de tela.
                onLoginSuccess();
            } else {
                setError("Usuário e/ou senha inválido(s)");
            }
        } catch (err) {
            console.error("Login API error:", err);
            setError("Erro de comunicação. Tente novamente.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        }
    };

    return (
        <div id="login-screen">
            <div className="login-card">
                <div className="login-logo">
                    <div className="brand">QR<span>369</span> Tools</div>
                    <div className="subtitle">Sistema de Gestão</div>
                </div>
                <hr className="login-divider" />
                <div className="form-group">
                    <label htmlFor="login-user">Usuário</label>
                    <input
                        type="text"
                        id="login-user"
                        placeholder="Digite seu usuário"
                        autoComplete="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        onKeyPress={handleKeyPress}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="login-pass">Senha</label>
                    <input
                        type="password"
                        id="login-pass"
                        placeholder="Digite sua senha"
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        onKeyPress={handleKeyPress}
                        required
                    />
                </div>
                {/* Exibe a mensagem de erro, se houver */}
                {error && <div className="login-error" style={{ display: 'block' }}>{error}</div>}
                
                <button
                    id="btn-entrar"
                    className="btn-primary"
                    type="button"
                    onClick={handleLogin}
                    disabled={isLoading} // Desabilita o botão durante o carregamento
                >
                    {isLoading ? 'Verificando...' : 'Entrar'}
                </button>
            </div>
        </div>
    );
}

export default LoginScreen;
