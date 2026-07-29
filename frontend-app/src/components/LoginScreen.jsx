import React from 'react';

function LoginScreen() {
    // A lógica de login será adicionada posteriormente
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
                    <input type="text" id="login-user" placeholder="Digite seu usuário" autoComplete="username" required />
                </div>
                <div className="form-group">
                    <label htmlFor="login-pass">Senha</label>
                    <input type="password" id="login-pass" placeholder="Digite sua senha" autoComplete="current-password" required />
                </div>
                <div id="login-error" className="login-error">Usuário e/ou senha inválido(s)</div>
                <button id="btn-entrar" className="btn-primary" type="button">Entrar</button>
            </div>
        </div>
    );
}

export default LoginScreen;
