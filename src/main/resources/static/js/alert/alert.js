/* alert.js */
const Alert = {
    // 모든 알림에 return을 붙여서 .then()이나 await를 쓸 수 있게 만듭니다.
    success: (msg, title = '성공') => {
        return Swal.fire({ icon: 'success', title, text: msg, confirmButtonColor: '#356fda' });
    },
    error: (msg, title = '오류') => {
        return Swal.fire({ icon: 'error', title, text: msg, confirmButtonColor: '#356fda' });
    },
    warning: (msg, title = '경고') => {
        return Swal.fire({ icon: 'warning', title, text: msg, confirmButtonColor: '#356fda' });
    },
    info: (msg, title = '알림') => {
        return Swal.fire({ icon: 'info', title, text: msg, confirmButtonColor: '#356fda' });
    },

    // confirm도 Promise를 반환하게 하여 async/await 사용이 가능하게 합니다.
    confirm: (msg, title = '확인') => {
        return Swal.fire({
            title: title,
            text: msg,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: '확인',
            cancelButtonText: '취소',
            confirmButtonColor: '#356fda',
            cancelButtonColor: '#aaa',
            reverseButtons: true
        });
    }
};