function showChatToast(message) {
    let container = document.getElementById('chat-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'chat-toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'chat-toast';
    toast.innerText = message;
    container.appendChild(toast);

    // 나타나기 애니메이션
    setTimeout(() => toast.classList.add('chat-show'), 10);

    // 2.5초 후 사라지기
    setTimeout(() => {
        toast.classList.remove('chat-show');
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}