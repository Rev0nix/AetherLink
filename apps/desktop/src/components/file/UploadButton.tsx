import { uploadPhoneFile } from "../../services/tauri";
import { open } from "@tauri-apps/plugin-dialog";

export default function UploadButton() {
  async function upload() {
    const serial = "X4LB6PPRIVZL9PRK";

    const local = await open({
      multiple: false,
    });

    if (!local || Array.isArray(local)) {
      return;
    }

    const remote =
      "/sdcard/Download/Revanth_HM_Resume.pdf";

    await uploadPhoneFile(
      serial,
      local,
      remote
    );

    alert("Upload Complete");
  }

  return (
    <button
      onClick={upload}
      className="rounded bg-green-800 px-4 py-2 text-white hover:bg-green-700"
    >
      📤 Upload File
    </button>
  );
}