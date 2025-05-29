document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('newImages');
    const fileListContainer = document.getElementById('file-list');
    const subjectInput = document.getElementById('subject');
    const contentInput = document.getElementById('content');
    const deleteImagesInput = document.getElementById('deleteImages');
    const updateBtn = document.getElementById('updateBtn');
    const checkbox = document.getElementById("notice-checkbox");

    // ✅ 처음 로드 시 제목, 내용 저장 (전역처럼 사용 가능)
    const originalSubject = subjectInput.value.trim();
    const originalContent = contentInput.value.trim();

    // 에러 메시지 처리
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
            fileInput.value = '';
        }
    });

    // 파일 리스트 업데이트 및 삭제 처리
    fileInput.addEventListener('change', function (e) {
        fileListContainer.innerHTML = '';
        let fileArray = Array.from(e.target.files);

        if (fileArray.length > 0) {
            fileArray.forEach(function (file) {
                const fileItem = document.createElement('div');
                fileItem.classList.add('d-flex', 'justify-content-between', 'align-items-center', 'my-2');

                const fileName = document.createElement('span');
                fileName.textContent = file.name;
                fileName.classList.add('file-name');

                const closeButton = document.createElement('button');
                closeButton.textContent = 'X';
                closeButton.classList.add('btn', 'btn-danger', 'btn-sm');
                closeButton.addEventListener('click', function () {
                    fileArray = fileArray.filter(f => f !== file);
                    fileListContainer.removeChild(fileItem);

                    const dataTransfer = new DataTransfer();
                    fileArray.forEach(f => dataTransfer.items.add(f));
                    fileInput.files = dataTransfer.files;
                });

                fileItem.appendChild(fileName);
                fileItem.appendChild(closeButton);
                fileListContainer.appendChild(fileItem);
            });
        }
    });

    // 체크박스 비활성화 막기
    if (checkbox) {
        checkbox.addEventListener("change", function () {
            if (!checkbox.checked) {
                alert("⚠️ 공지글은 수정 또는 삭제만 가능합니다.");
                checkbox.checked = true;
            }
        });
    }

    // 수정 버튼 클릭 시 체크된 이미지가 남아있으면 얼럿 + 수정사항 없는지 체크
    updateBtn.addEventListener('click', function (e) {
        const deleteImagesValue = deleteImagesInput.value;
        const checkedCheckboxes = document.querySelectorAll('.delete-checkbox:checked');
        const currentSubject = subjectInput.value.trim();
        const currentContent = contentInput.value.trim();
        const newImagesCount = fileInput.files.length;

        // ✅ 체크박스 체크만 하고 삭제 버튼 안 누른 경우
        if (checkedCheckboxes.length > 0 && !deleteImagesValue) {
            alert("선택한 이미지가 있습니다.\n'선택한 이미지 삭제' 버튼을 눌러주세요.");
            e.preventDefault();
            return;
        }

        // ✅ 이미지 변경 여부 체크
        const isImageDeleted = deleteImagesValue.length > 0;
        const isNewImageAdded = newImagesCount > 0;
        const isImageChanged = isImageDeleted || isNewImageAdded;

        // ✅ 제목, 내용, 이미지 모두 변경 없을 때
        if (
            currentSubject === originalSubject &&
            currentContent === originalContent &&
            !isImageChanged
        ) {
            alert("수정된 내용이 없습니다.");
            e.preventDefault();
            return;
        }

        // ✅ 변경사항 있음
        alert("변경사항을 저장합니다.");
    });

    document.querySelectorAll('.polaroid').forEach(polaroid => {
        polaroid.addEventListener('click', function (event) {
            const checkbox = polaroid.querySelector('input[type="checkbox"]');
            if (!event.target.matches('input[type="checkbox"]') && !event.target.closest('label')) {
                checkbox.checked = !checkbox.checked;
            }
        });
    });
});

// removeSelectedImages는 그대로 유지
function removeSelectedImages() {
    const checkboxes = document.querySelectorAll('.delete-checkbox:checked');
    const deletedIds = [];

    if (checkboxes.length === 0) {
        alert("삭제할 이미지를 선택해주세요.");
        return;
    }

    checkboxes.forEach(checkbox => {
        deletedIds.push(checkbox.value);

        const polaroidDiv = checkbox.closest('.polaroid');
        if (polaroidDiv) {
            polaroidDiv.style.display = 'none';
        }
    });

    document.getElementById('deleteImages').value = deletedIds.join(',');

    alert("선택한 이미지가 삭제되었습니다.\n변경사항을 저장하려면 '수정' 버튼을 눌러주세요.");
}
