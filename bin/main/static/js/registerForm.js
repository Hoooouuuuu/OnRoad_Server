function isValidUserId(userId) {
  // 아이디는 영문 소문자 + 숫자, 5~16자 제한
  const userIdPattern = /^[a-z0-9]{5,16}$/;
  return userIdPattern.test(userId);
}

$(document).ready(function () {
  let isIdAvailable = false;
  let isNickNameAvailable = false;

  $("#checkDuplicate").click(function () {
    const userId = $("#userId").val().trim();

    if (!userId) {
      $("#duplicateMessage").text("아이디를 입력해주세요.").css("color", "red").show();
      return;
    }

    if (!isValidUserId(userId)) {
      $("#duplicateMessage").text("아이디는 5~16자의 영소문자와 숫자만 사용 가능합니다.").css("color", "red").show();
      return;
    }

    $.get("/register/checkIdDuplicate?userId=" + userId, function (data) {
      if (data) {
        $("#duplicateMessage").text("이미 사용 중인 아이디입니다.").css("color", "red").show();
        isIdAvailable = false;
        $("#userId").focus();
      } else {
        $("#duplicateMessage").text("사용 가능한 아이디입니다.").css("color", "green").show();
        isIdAvailable = true;
      }
    });
  });

  $("#checkNickNameDuplicate").click(function () {
    const nickName = $("#nickName").val().trim();
    if (!nickName) {
      $("#nickNameDuplicateMessage").text("닉네임을 입력해주세요.").css("color", "red").show();
      return;
    }

    $.get("/register/checkNickNameDuplicate?nickName=" + nickName, function (data) {
      if (data) {
        $("#nickNameDuplicateMessage").text("이미 사용 중인 닉네임입니다.").css("color", "red").show();
        isNickNameAvailable = false;
        $("#nickName").focus();
      } else {
        $("#nickNameDuplicateMessage").text("사용 가능한 닉네임입니다.").css("color", "green").show();
        isNickNameAvailable = true;
      }
    });
  });

  $("#password, #confirmPassword").on("keyup", function () {
    const password = $("#password").val();
    const confirmPassword = $("#confirmPassword").val();

    if (!password || !confirmPassword) {
      $("#passwordMatchMessage").text("").hide();
      return;
    }

    if (password === confirmPassword) {
      $("#passwordMatchMessage").text("비밀번호가 일치합니다.").css("color", "green").show();
    } else {
      $("#passwordMatchMessage").text("비밀번호가 일치하지 않습니다.").css("color", "red").show();
    }
  });

  $("form").submit(function () {
    if (!isIdAvailable) {
      alert("아이디 중복 확인을 해주세요.");
      return false;
    }

    if (!isNickNameAvailable) {
      alert("닉네임 중복 확인을 해주세요.");
      return false;
    }

    const password = $("#password").val();
    const confirmPassword = $("#confirmPassword").val();
    if (password !== confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      $("#confirmPassword").focus();
      return false;
    }

    return true; // 나머지는 서버에서 검증
  });
  $("form").submit(function () {
    const name = $("#name").val().trim();
    const birthDate = $("#birthDate").val();
    const phoneNumber = $("#phoneNumber").val().trim();
    const addressCity = $("#addressCity").val().trim();
    const addressDistrict = $("#addressDistrict").val().trim();
    const addressRoad = $("#addressRoad").val().trim();
    const addressNumber = $("#addressNumber").val().trim();
    const addressDetail = $("#addressDetail").val().trim();

    const phonePattern = /^01([0|1|6|7|8|9])-?\d{3,4}-?\d{4}$/;

    if (!name) {
      alert("이름을 입력해주세요.");
      $("#name").focus();
      return false;
    }

    if (!birthDate) {
      alert("생년월일을 입력해주세요.");
      $("#birthDate").focus();
      return false;
    }

    if (!phonePattern.test(phoneNumber)) {
      alert("올바른 전화번호 형식을 입력해주세요. 예: 010-1234-5678");
      $("#phoneNumber").focus();
      return false;
    }

    if (!addressCity || !addressDistrict || !addressRoad || !addressNumber || !addressDetail) {
      alert("주소를 모두 입력해주세요.");
      $("#addressDetail").focus();
      return false;
    }

    return true;
  });
});

function execDaumPostcode() {
  new daum.Postcode({
    oncomplete: function (data) {
      let fullRoad = data.roadAddress;
      if (!fullRoad) {
        alert("도로명 주소를 찾을 수 없습니다.");
        return;
      }

      const split = fullRoad.split(" ");
      if (split.length < 4) {
        alert("주소 형식이 예상과 다릅니다.");
        return;
      }

      document.getElementById("addressCity").value = split[0];
      document.getElementById("addressDistrict").value = split[1];
      document.getElementById("addressRoad").value = split[2];
      document.getElementById("addressNumber").value = split[3];
      document.getElementById("addressDetail").value = "";
      document.getElementById("addressDetail").focus();
    }
  }).open();
}