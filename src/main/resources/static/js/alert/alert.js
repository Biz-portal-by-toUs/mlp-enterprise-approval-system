/* alert.js */
const Alert = {
    // 성공 알림
    success: (msg, title = '성공') => {
        return Swal.fire({
            icon: 'success',
            title: title,
            text: msg,
            confirmButtonColor: '#356fda'
        });
    },

    // 오류 알림
    error: (msg, title = '오류') => {
        return Swal.fire({
            icon: 'error',
            title: title,
            text: msg,
            confirmButtonColor: '#356fda'
        });
    },

    // 경고 알림
    warning: (msg, title = '경고') => {
        return Swal.fire({
            icon: 'warning',
            title: title,
            text: msg,
            confirmButtonColor: '#356fda'
        });
    },

    // 정보 알림 (로그인 폼에서 사용하는 것)
    info: (msg, title = '알림') => {
        return Swal.fire({
            icon: 'info',
            title: title,
            text: msg,
            confirmButtonColor: '#356fda'
        });
    },

    // 확인창 (Promise 기반으로 개선)
    /* alert.js */
    confirm: (msg, title = '확인') => {
        return Swal.fire({
            title: title,
            text: msg,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            confirmButtonColor: '#356fda', // ✅ 여기서 색상을 직접 지정
            cancelButtonColor: '#aaa',     // ✅ 취소 버튼 색상
            reverseButtons: true,
            // CSS 클래스가 확실히 먹히게 하려면 아래 코드 추가 (선택사항)
            customClass: {
                confirmButton: 'my-swal-confirm-btn',
                cancelButton: 'my-swal-cancel-btn'
            }
        });
    }
};