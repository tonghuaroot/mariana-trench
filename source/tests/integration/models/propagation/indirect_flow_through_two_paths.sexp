(method (private) "LFlow;.into_sink:(LData;)V"
 (
  (load-param v0)
  (load-param v1)

  LINE(1)
  (invoke-virtual (v0 v1) "LSink;.sink:(LData;)V")
  (return-void)
 )
)
(method (private) "LFlow;.into_into_sink_one:(LData;)V"
 (
  (load-param v0)
  (load-param v1)

  LINE(1)
  (invoke-virtual (v0 v1) "LFlow;.into_sink:(LData;)V")
  (return-void)
 )
)
(method (private) "LFlow;.into_into_sink_two:(LData;)V"
 (
  (load-param v0)
  (load-param v1)

  LINE(1)
  (invoke-virtual (v0 v1) "LFlow;.into_sink:(LData;)V")
  (return-void)
 )
)
(method (private) "LFlow;.indirect:()V"
 (
  LINE(1)
  (invoke-virtual (v0) "LSource;.source:()LData;")
  (move-result-object v1)

  LINE(2)
  (invoke-virtual (v0 v1) "LFlow;.into_into_sink_one:(LData;)V")
  (invoke-virtual (v0 v1) "LFlow;.into_into_sink_two:(LData;)V")
  (return-void)
 )
)
