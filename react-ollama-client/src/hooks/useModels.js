import { useState, useEffect, useCallback } from 'react';
import { fetchModelsApi } from '../api/ollama';

const MODEL_STORAGE_KEY = 'ollama_selected_model';

export const useModels = () => {
  const [models, setModels] = useState([]);
  const [selectedModel, setSelectedModel] = useState('');
  const [error, setError] = useState(null);

  useEffect(() => {
    let isMounted = true; // Avoid setting state if component unmounts during fetch

    const fetchModels = async () => {
      const savedModel = localStorage.getItem(MODEL_STORAGE_KEY);
      try {
        const data = await fetchModelsApi();
        if (isMounted) {
          setModels(data.models || []);
          if (savedModel) {
            setSelectedModel(savedModel);
          } else if (data.models && data.models.length > 0) {
            const defaultModel = data.models[0].name;
            setSelectedModel(defaultModel);
            localStorage.setItem(MODEL_STORAGE_KEY, defaultModel);
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(err);
          console.error("Failed to fetch models:", err);
        }
      }
    };

    fetchModels();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleModelChange = useCallback((newModel) => {
    setSelectedModel(newModel);
    localStorage.setItem(MODEL_STORAGE_KEY, newModel);
  }, []);

  return { models, selectedModel, handleModelChange, error };
};
