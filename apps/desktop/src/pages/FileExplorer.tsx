import { useEffect, useState } from "react";
import { listPhoneFiles } from "../services/tauri";
import type { PhoneFile } from "../types/file";
import { save } from "@tauri-apps/plugin-dialog";
import { downloadPhoneFile } from "../services/tauri";
import FileToolbar from "../components/file/FileToolbar";
import Breadcrumb from "../components/file/Breadcrumb";
import FileList from "../components/file/FileList";
import UploadButton from "../components/file/UploadButton";

export default function FileExplorer() {
    const serial = "X4LB6PPRIVZL9PRK";

    const [path, setPath] = useState("/sdcard");

    const [files, setFiles] = useState<PhoneFile[]>([]);

    async function refresh() {
        const result = await listPhoneFiles(serial, path);

        console.log(result);

        alert(JSON.stringify(result.slice(0, 5), null, 2));
        setFiles(result);
    }

    useEffect(() => {
        void refresh();
    }, [path]);

    return (
        <div className="space-y-6">

            <h1
                className="text-4xl font-bold"
                style={{ color: "zinc-500" }}
            >
                FILE EXPLORER
            </h1>

            <FileToolbar
                onRefresh={refresh}
                onUp={() => {
                    if (path === "/sdcard") return;

                    const parent =
                        path.substring(
                            0,
                            path.lastIndexOf("/")
                        );

                    setPath(parent);
                }}
            />

            <UploadButton />

            <Breadcrumb path={path} />

            <FileList
                files={files}
                onOpen={async (file) => {
                    if (file.is_directory) {
                        setPath(file.path);
                        return;
                    }

                    alert(`FILE CLICKED: ${file.name}`);
                }

                }
            />

        </div>
    );
}