import { createBrowserRouter } from "react-router-dom";
import DeviceDetails from "../pages/DeviceDetails";

import Dashboard from "../pages/Dashboard";
import Devices from "../pages/Devices";
import Clipboard from "../pages/Clipboard";
import Notifications from "../pages/Notifications";
import Settings from "../pages/Settings";

import MainLayout from "../layouts/MainLayout";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      {
        path: "device/:id",
        element: <DeviceDetails />,
      },
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: "devices",
        element: <Devices />,
      },
      {
        path: "clipboard",
        element: <Clipboard />,
      },
      {
        path: "notifications",
        element: <Notifications />,
      },
      {
        path: "settings",
        element: <Settings />,
      },
    ],
  },
]);