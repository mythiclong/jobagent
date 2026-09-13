# 简历分析输出约束

<!-- prompt-id: resume-analysis-output -->
<!-- prompt-version: v1 -->

模型必须返回一个 JSON 对象，字段固定为：

```json
{
  "summary": "总体诊断",
  "coreProblems": ["问题 1", "问题 2"],
  "missingKeywords": ["关键词 1", "关键词 2", "关键词 3"],
  "improvementSuggestions": ["建议 1", "建议 2", "建议 3"]
}
```

字段约束：

- `summary` 是字符串；没有可靠分析时返回空字符串。
- `coreProblems` 是字符串数组，最多 4 项。
- `missingKeywords` 是字符串数组，最多 12 项。
- `improvementSuggestions` 是字符串数组，最多 5 项。
- 数组项目必须去重、非空、与当前简历和目标岗位相关。
- 禁止在 JSON 外输出任何文本。
