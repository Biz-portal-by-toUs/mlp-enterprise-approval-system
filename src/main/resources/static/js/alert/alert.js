/* alert.js */
const Alert = {
    success: (msg, title = '성공') => Swal.fire(title, msg, 'success'),
    error: (msg, title = '오류') => Swal.fire(title, msg, 'error'),
    warning: (msg, title = '경고') => Swal.fire(title, msg, 'warning'),
    info: (msg, title = '알림') => Swal.fire(title, msg, 'info'),
    confirm: (msg, title, onConfirm, onCancel) => {
        if (typeof title === 'function') {
            onCancel = onConfirm;
            onConfirm = title;
            title = '확인';
        }
        Swal.fire({
            title: title,
            text: msg,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed) {
                if (onConfirm) onConfirm();
            } else if (result.dismiss === Swal.DismissReason.cancel) {
                if (onCancel) onCancel();
            }
        });
    }
};