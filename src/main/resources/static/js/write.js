document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('images');
    const fileListContainer = document.getElementById('file-list');

    // 에러 메시지 처리 (if 존재하면 alert)
    const errorMessage = /*[[${error}]]*/ '';
    if (errorMessage) {
        alert(errorMessage);
    }

    // 이미지 파일 검증
    fileInput.addEventListener('change', function () {
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/webp'];
        const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
        const files = fileInput.files;
        let hasInvalidFile = false;

        for (let file of files) {
            const ext = file.name.split('.').pop().toLowerCase();
            const type = file.type;

            if (!allowedExtensions.includes(ext) || !allowedTypes.includes(type)) {
                hasInvalidFile = true;
                break;
            }
        }

        if (hasInvalidFile) {
            alert('이미지 파일만 업로드 가능합니다! (jpg, png, gif 등)');
            fileInput.value = ''; // 파일 초기화해서 업로드 막기
        }
    });

    // 파일 리스트 업데이트 및 삭제 처리
    fileInput.addEventListener('change', function (e) {
        fileListContainer.innerHTML = '';  // 기존 목록 초기화

        const files = e.target.files;
        let fileArray = Array.from(files); // File 객체를 배열로 변환

        if (fileArray.length > 0) {
            fileArray.forEach(function (file, index) {
                const fileItem = document.createElement('div');
                fileItem.classList.add('d-flex', 'justify-content-between', 'align-items-center', 'my-2');

                const fileName = document.createElement('span');
                fileName.textContent = file.name;
                fileName.classList.add('file-name');

                const closeButton = document.createElement('button');
                closeButton.textContent = 'X';
                closeButton.classList.add('btn', 'btn-danger', 'btn-sm');
                closeButton.addEventListener('click', function () {
                    // "X" 클릭 시 파일 삭제
                    fileArray = fileArray.filter(f => f !== file); // 파일 배열에서 해당 파일을 제거
                    fileListContainer.removeChild(fileItem); // DOM에서 해당 파일 아이템 제거

                    // 파일 input의 files를 업데이트
                    const dataTransfer = new DataTransfer();
                    fileArray.forEach(f => dataTransfer.items.add(f));
                    fileInput.files = dataTransfer.files; // 새로운 파일 목록을 설정
                });

                fileItem.appendChild(fileName);
                fileItem.appendChild(closeButton);
                fileListContainer.appendChild(fileItem);
            });
        }
    });
});

function removeSelectedImages() {
    const checkboxes = document.querySelectorAll('input[name="deleteImages"]:checked');

    checkboxes.forEach(checkbox => {
        // 부모 div는 화면에서 숨기기만 (실제로는 제거 안 함)
        const imageDiv = checkbox.closest('div');
        if (imageDiv) {
            imageDiv.style.display = 'none';
        }
    });
}