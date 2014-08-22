package model

sealed trait TestResult

case object Passed extends TestResult
case object Failed extends TestResult
