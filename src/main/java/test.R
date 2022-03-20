T <<- function(){
  print(1)
}
H <- function (){
  T()
  length(c(1:5))
}
K <- function(){
  H <- 3
}
H()