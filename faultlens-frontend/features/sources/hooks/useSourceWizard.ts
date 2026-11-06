import { useState } from "react";
import toast from "react-hot-toast";
import { useTranslations } from "next-intl";
import { LogSourceType } from "@/shared/types/common.types";
import {
  useTestConfigMutation,
  useCreateSourceMutation,
} from "@/features/sources/api/useSourceMutation";

export function useSourceWizard(onClose: () => void) {
  const t = useTranslations("sources.toast");
  const [step, setStep] = useState(1);
  const [selectedType, setSelectedType] = useState<LogSourceType | null>(null);
  const [name, setName] = useState("");
  const [host, setHost] = useState("");
  const [namespace, setNamespace] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [logFilePath, setLogFilePath] = useState("");
  const [testResult, setTestResult] = useState<"success" | "error" | null>(null);

  const testConfig = useTestConfigMutation();
  const createSource = useCreateSourceMutation();

  const handleTypeSelect = (type: LogSourceType) => {
    setSelectedType(type);
    setStep(2);
  };

  const buildConfigObj = () => {
    let configObj: Record<string, string> = {};
    if (selectedType === LogSourceType.SSH) {
      configObj = { host, username, password, logFilePath };
    } else if (selectedType === LogSourceType.DOCKER) {
      configObj = { dockerHost: host, containerId: logFilePath };
    } else if (selectedType === LogSourceType.KUBERNETES) {
      configObj = { namespace };
    } else if (selectedType === LogSourceType.LOCAL_FILE) {
      configObj = { logFilePath };
    }
    return configObj;
  };

  const handleTest = async () => {
    setTestResult(null);
    try {
      await testConfig.mutateAsync({
        name,
        type: selectedType!,
        config: JSON.stringify(buildConfigObj()),
      });
      setTestResult("success");
    } catch {
      setTestResult("error");
    }
  };

  const handleSave = async () => {
    try {
      await createSource.mutateAsync({
        name,
        type: selectedType!,
        config: JSON.stringify(buildConfigObj()),
      });
      handleClose();
    } catch (error) {
      // e.g. LOCAL_FILE_PATH_NOT_ALLOWED: path outside COLLECTOR_FILE_ALLOWED_DIRS
      const apiError = (error as { response?: { data?: { error?: { message?: string } } } })
        ?.response?.data?.error;
      toast.error(apiError?.message ?? t("saveFailed"));
    }
  };

  const handleClose = () => {
    setStep(1);
    setSelectedType(null);
    setName("");
    setHost("");
    setNamespace("");
    setUsername("");
    setPassword("");
    setLogFilePath("");
    setTestResult(null);
    onClose();
  };

  return {
    // State
    step,
    setStep,
    selectedType,
    name,
    setName,
    host,
    setHost,
    namespace,
    setNamespace,
    username,
    setUsername,
    password,
    setPassword,
    logFilePath,
    setLogFilePath,
    testResult,
    setTestResult,

    // Mutations state
    isTesting: testConfig.isPending,
    isSaving: createSource.isPending,

    // Actions
    handleTypeSelect,
    handleTest,
    handleSave,
    handleClose,
  };
}
